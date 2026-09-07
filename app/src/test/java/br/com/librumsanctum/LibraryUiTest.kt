package br.com.librumsanctum

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LibraryUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var application: Application
    @Before fun setup() {
        application = ApplicationProvider.getApplicationContext()
        File(application.filesDir, "library").deleteRecursively()
        application.getSharedPreferences("reader", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun emptyLibraryOffersImport() {
        val model = LibraryViewModel(application)
        compose.setContent { LibrumApp(model) }
        compose.onNodeWithText("Escolher um livro").assertIsDisplayed()
        compose.onNodeWithText("Uma estante de possibilidades").assertIsDisplayed()
        screenshot("empty-library")
    }
    @Test fun opensBookCustomizesReaderAndReturnsToLibrary() {
        val repository = LibraryRepository(application)
        repository.save(Book("fixture", "Um livro de teste", "Autora", "EPUB", 2, 0))
        File(application.filesDir, "library/fixture.json").writeText("""[{"text":"Primeiro trecho da história.","page":0},{"text":"Último trecho da história.","page":0}]""")
        val model = LibraryViewModel(application)
        compose.setContent { LibrumApp(model) }
        compose.waitUntil(10_000) { model.state.value.books.isNotEmpty() && !model.state.value.busy }
        compose.waitForIdle()
        compose.onNodeWithTag("book-fixture").performClick()
        compose.waitUntil(10_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            check(model.state.value.error == null) { model.state.value.error!! }
            model.state.value.selected != null && !model.state.value.busy
        }
        compose.onNodeWithText("Primeiro trecho da história.").assertIsDisplayed()
        screenshot("reader-sepia")
        compose.onNodeWithText("Aa").performClick()
        compose.onNodeWithText("Escuro").performClick()
        compose.onNodeWithText("Voltar à leitura").performClick()
        compose.onNodeWithText("‹ Estante").performClick()
        compose.onNodeWithText("RETOME SUA HISTÓRIA").assertIsDisplayed()
        compose.onNodeWithText("Continuar →").assertIsDisplayed()
        screenshot("library-dark")
        org.junit.Assert.assertEquals("Escuro", repository.settings().theme)
    }
    @Test fun swipesPagesAndPreservesAnchorAcrossModesReopeningAndFontChanges() {
        val passages = (1..16).map { index -> Passage("Trecho $index. " + "Uma história sobre livros e memórias. ".repeat(12)) }
        val model = openFixture(passages)
        chooseMode("Páginas")
        awaitPager()
        compose.onNodeWithContentDescription("Página anterior").assertIsNotEnabled()
        val firstCounter = counter()
        compose.onNodeWithTag("paged-reader").performTouchInput { swipeLeft() }
        awaitCondition { anchor(model) > TextAnchor(0) }
        org.junit.Assert.assertNotEquals(firstCounter, counter())
        val secondPageAnchor = anchor(model)
        org.junit.Assert.assertTrue("A página deve poder começar dentro de um trecho", secondPageAnchor.offset > 0)
        screenshot("reader-paged")
        compose.onNodeWithTag("paged-reader").performTouchInput { swipeRight() }
        awaitCondition { anchor(model) == TextAnchor(0) }
        compose.onNodeWithContentDescription("Próxima página").performClick()
        awaitCondition { anchor(model) == secondPageAnchor }
        val secondCounter = counter()
        awaitCondition {
            model.repository.books().single().let { TextAnchor(it.position, it.positionOffset) } == secondPageAnchor
        }
        compose.onNodeWithText("‹ Estante").performClick()
        compose.onNodeWithText("Continuar →").performClick()
        awaitCondition { model.state.value.selected != null && !model.state.value.busy }
        awaitPager()
        org.junit.Assert.assertEquals(secondCounter, counter())
        org.junit.Assert.assertEquals(secondPageAnchor, anchor(model))
        chooseMode("Rolagem")
        compose.onNodeWithTag("scroll-reader").assertIsDisplayed()
        org.junit.Assert.assertEquals(secondPageAnchor, anchor(model))
        compose.onNodeWithTag("scroll-reader").performTouchInput { swipeUp() }
        awaitCondition { anchor(model) > secondPageAnchor }
        val scrolledAnchor = anchor(model)
        chooseMode("Páginas")
        awaitPager()
        org.junit.Assert.assertEquals(scrolledAnchor, anchor(model))
        compose.runOnIdle { model.settings(model.state.value.settings.copy(size = 32f)) }
        awaitPager()
        org.junit.Assert.assertEquals(scrolledAnchor, anchor(model))
        org.junit.Assert.assertEquals(ReadingMode.PAGED, LibraryRepository(application).settings().mode)
        screenshot("reader-paged-large-font")
    }
    @Test fun singlePageStopsAtBoundariesAndKeepsCompletionOnReopen() {
        val model = openFixture(listOf(Passage("Uma breve história, completa em uma página.")))
        chooseMode("Páginas")
        awaitPager()
        compose.onNodeWithText("Página 1 de 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Página anterior").assertIsNotEnabled()
        compose.onNodeWithTag("paged-reader").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Página 1 de 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Concluir leitura").performClick()
        awaitCondition { model.state.value.selected!!.finished }
        compose.onNodeWithText("‹ Estante").performClick()
        compose.onNodeWithText("Continuar →").performClick()
        awaitCondition { model.state.value.selected != null && !model.state.value.busy }
        awaitPager()
        org.junit.Assert.assertTrue(model.state.value.selected!!.finished)
        compose.onNodeWithText("100% lido").assertIsDisplayed()
    }
    private fun openFixture(passages: List<Passage>): LibraryViewModel {
        val repository = LibraryRepository(application)
        repository.save(Book("pages", "Entre páginas e memórias", "Biblioteca de teste", "EPUB", passages.size, 0))
        File(application.filesDir, "library/pages.json").writeText(org.json.JSONArray().apply {
            passages.forEach { put(org.json.JSONObject().put("text", it.text).put("page", 0)) }
        }.toString())
        val model = LibraryViewModel(application)
        compose.setContent { LibrumApp(model) }
        awaitCondition { model.state.value.books.isNotEmpty() && !model.state.value.busy }
        compose.onNodeWithTag("book-pages").performClick()
        awaitCondition {
            check(model.state.value.error == null) { model.state.value.error!! }
            model.state.value.selected != null && !model.state.value.busy
        }
        return model
    }
    private fun chooseMode(label: String) {
        compose.onNodeWithText("Aa").performClick()
        compose.onNodeWithText(label).performClick()
        compose.onNodeWithText("Voltar à leitura").performClick()
    }
    private fun anchor(model: LibraryViewModel) = model.state.value.selected!!.let { TextAnchor(it.position, it.positionOffset) }
    private fun counter() = compose.onNodeWithTag("page-counter").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
    private fun awaitCondition(condition: () -> Boolean) {
        compose.waitUntil(30_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            condition()
        }
        compose.waitForIdle()
    }
    private fun awaitPager() {
        awaitCondition { compose.onAllNodesWithTag("page-counter").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        val file = File("build/outputs/screenshots/$name.png").apply { parentFile!!.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
