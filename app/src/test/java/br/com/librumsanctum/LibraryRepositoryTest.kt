package br.com.librumsanctum

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LibraryRepositoryTest {
    private lateinit var context: Context
    private lateinit var repository: LibraryRepository
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "library").deleteRecursively()
        context.getSharedPreferences("reader", Context.MODE_PRIVATE).edit().clear().commit()
        PDFBoxResourceLoader.init(context)
        repository = LibraryRepository(context)
    }
    @Test fun restoresIndependentPositionsAndPreferencesAfterReopening() {
        val a = Book("a", "Primeiro", "Autor", "EPUB", 80, 0, position = 20, lastRead = 100)
        val b = Book("b", "Segundo", "Autor", "PDF", 100, 30, position = 32, pdfPage = 19, original = true, lastRead = 200)
        repository.save(a); repository.save(b)
        repository.saveSettings(ReadingSettings("Escuro", 26f, false))
        val reopened = LibraryRepository(context)
        assertEquals(setOf(a, b), reopened.books().toSet())
        assertEquals(ReadingSettings("Escuro", 26f, false), reopened.settings())
    }
    @Test fun recoversAtomicBackupAfterInterruptedIndexWrite() {
        val book = Book("a", "Preservado", "Autor", "EPUB", 10, 0, position = 4)
        repository.save(book)
        val index = File(context.filesDir, "library/index.json")
        assertTrue(index.renameTo(File(context.filesDir, "library/index.json.bak")))
        assertEquals(listOf(book), LibraryRepository(context).books())
    }
    private fun pdf(text: Boolean): File = File(context.cacheDir, "sample.pdf").apply {
        PDDocument().use { document ->
            val page = PDPage(); document.addPage(page)
            if (text) PDPageContentStream(document, page).use { stream ->
                stream.beginText(); stream.setFont(PDType1Font.HELVETICA, 14f)
                stream.newLineAtOffset(40f, 700f); stream.showText("Librum offline reading."); stream.endText()
            }
            document.save(this)
        }
    }
    @Test fun importsRealPdfCopiesSourceAndDeduplicatesWithoutResettingPosition() {
        val source = pdf(true)
        val book = repository.import(Uri.fromFile(source))
        assertEquals("PDF", book.format)
        assertTrue(repository.passages(book).any { it.text.contains("Librum offline") })
        val finished = book.copy(finished = true, lastRead = 42)
        repository.save(finished)
        assertEquals(finished, repository.import(Uri.fromFile(source)))
        source.delete()
        assertTrue(repository.source(book).exists())
        assertEquals(1, repository.books().size)
    }
    @Test fun imageOnlyPdfUsesOriginalReader() {
        val book = repository.import(Uri.fromFile(pdf(false)))
        assertTrue(book.original); assertEquals(0, book.passages); assertEquals(1, book.pages)
    }
    @Test fun invalidImportDoesNotChangeLibraryOrLeaveTempFiles() {
        val invalid = File(context.cacheDir, "invalid.pdf").apply { writeText("not a pdf") }
        assertThrows(IllegalStateException::class.java) { repository.import(Uri.fromFile(invalid)) }
        assertTrue(repository.books().isEmpty())
        assertFalse(File(context.filesDir, "library").listFiles()!!.any { it.name.startsWith("import-") })
    }
}
