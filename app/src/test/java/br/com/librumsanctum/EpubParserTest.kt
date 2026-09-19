package br.com.librumsanctum

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubParserTest {
    private fun epub(entries: Map<String, String>): File = File.createTempFile("book", ".epub").apply {
        deleteOnExit()
        ZipOutputStream(outputStream()).use { zip -> entries.forEach { (path, body) ->
            zip.putNextEntry(ZipEntry(path)); zip.write(body.toByteArray()); zip.closeEntry()
        } }
    }
    private fun entries(href: String = "text/one.xhtml") = mapOf(
        "META-INF/container.xml" to "<container><rootfiles><rootfile full-path='OPS/book.opf'/></rootfiles></container>",
        "OPS/book.opf" to """<package xmlns:dc="http://purl.org/dc/elements/1.1/"><metadata><dc:title>Meu livro</dc:title><dc:creator>Autora</dc:creator></metadata><manifest><item id="two" href="text/two.xhtml"/><item id="one" href="$href"/></manifest><spine><itemref idref="one"/><itemref idref="two"/></spine></package>""",
        "OPS/text/one.xhtml" to "<html><body><h1>Primeiro</h1><p>Olá &amp; mundo.</p><script>invisível</script></body></html>",
        "OPS/text/two.xhtml" to "<html><body><p>Segundo</p></body></html>",
    )
    @Test fun followsSpineAndExtractsMetadataAndEntities() {
        val parsed = EpubParser.parse(epub(entries()))
        assertEquals("Meu livro", parsed.title); assertEquals("Autora", parsed.author)
        assertEquals(listOf("Primeiro", "Olá & mundo.", "Segundo"), parsed.passages.map { it.text })
    }
    @Test fun rejectsPathsOutsideArchive() {
        assertThrows(IllegalArgumentException::class.java) { EpubParser.parse(epub(entries("../../outside.xhtml"))) }
    }
    @Test fun reportsMissingChapter() {
        assertThrows(IllegalStateException::class.java) { EpubParser.parse(epub(entries("missing.xhtml"))) }
    }
    @Test fun rejectsEncryptedBooks() {
        assertThrows(IllegalArgumentException::class.java) { EpubParser.parse(epub(entries() + ("META-INF/encryption.xml" to "<encryption/>"))) }
    }
    @Test fun preservesLongParagraphWithoutArtificialBreaks() {
        val source = (1..200).joinToString(" ") { "palavra$it" }
        val passages = splitPassages(source, 7)
        assertEquals(1, passages.size)
        assertEquals(source, passages.joinToString(" ") { it.text })
        assertTrue(passages.all { it.sourcePage == 7 })
    }
    @Test fun preservesNestedParagraphsBreaksAndLooseTextInReadingOrder() {
        val chapter = """<body>Introdução.<blockquote><p>Primeiro <em>parágrafo</em>.</p><p>Segundo.</p></blockquote><div>— Olá!<br/>— Tudo bem?</div><p>Fim.</p></body>"""
        val parsed = EpubParser.parse(epub(entries() + ("OPS/text/one.xhtml" to chapter)))
        assertEquals(listOf("Introdução.", "Primeiro parágrafo.", "Segundo.", "— Olá!", "— Tudo bem?", "Fim.", "Segundo"), parsed.passages.map { it.text })
    }
    @Test fun distinguishesWrappedLinesDialogueAndParagraphs() {
        val source = "Um parágrafo que continua\nna linha seguinte — sem separar o inciso.\n— Olá!\n— Tudo bem?\n\nOutro parágrafo."
        assertEquals(listOf("Um parágrafo que continua na linha seguinte — sem separar o inciso.", "— Olá!", "— Tudo bem?", "Outro parágrafo."), splitPassages(source).map { it.text })
    }
    @Test fun progressIsBoundedForEmptyAndCompletedBooks() {
        assertEquals(0f, Book("a", "A", "B", "EPUB", 0, 0).progress)
        assertEquals(1f, Book("a", "A", "B", "EPUB", 2, 0, finished = true).progress)
    }
}
