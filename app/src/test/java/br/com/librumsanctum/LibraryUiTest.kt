package br.com.librumsanctum

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
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
