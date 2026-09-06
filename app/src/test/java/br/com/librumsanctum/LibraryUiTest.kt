package br.com.librumsanctum

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp")
class LibraryUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var application: Application
    @Before fun setup() {
        application = ApplicationProvider.getApplicationContext()
        File(application.filesDir, "library").deleteRecursively()
    }
    @Test fun emptyLibraryOffersImport() {
        compose.setContent { LibrumApp(LibraryViewModel(application)) }
        compose.onNodeWithText("Escolher um livro").assertIsDisplayed()
        compose.onNodeWithText("Uma estante de possibilidades").assertIsDisplayed()
    }
    @Test fun opensBookCustomizesReaderAndReturnsToLibrary() {
        val repository = LibraryRepository(application)
        repository.save(Book("fixture", "Um livro de teste", "Autora", "EPUB", 2, 0))
        File(application.filesDir, "library/fixture.json").writeText("""[{"text":"Primeiro trecho da história.","page":0},{"text":"Último trecho da história.","page":0}]""")
        val model = LibraryViewModel(application)
        compose.setContent { LibrumApp(model) }
        compose.waitUntil(10_000) { model.state.value.books.isNotEmpty() && !model.state.value.busy }
        compose.onAllNodesWithText("Um livro de teste")[0].performClick()
        compose.waitUntil(10_000) { model.state.value.selected != null && !model.state.value.busy }
        compose.onNodeWithText("Primeiro trecho da história.").assertIsDisplayed()
        compose.onNodeWithText("Aa").performClick()
        compose.onNodeWithText("Escuro").performClick()
        compose.onNodeWithText("Voltar à leitura").performClick()
        compose.onNodeWithText("‹ Estante").performClick()
        compose.onNodeWithText("RETOME SUA HISTÓRIA").assertIsDisplayed()
        compose.onNodeWithText("Continuar →").assertIsDisplayed()
        org.junit.Assert.assertEquals("Escuro", repository.settings().theme)
    }
}
