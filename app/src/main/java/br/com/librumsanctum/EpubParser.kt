package br.com.librumsanctum

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.zip.ZipFile

data class ParsedBook(val title: String, val author: String, val passages: List<Passage>, val pages: Int)

object EpubParser {
    private const val MAX_ENTRY = 16 * 1024 * 1024
    fun parse(file: File): ParsedBook = ZipFile(file).use { zip ->
        var consumed = 0L
        fun read(path: String): String {
            val entry = zip.getEntry(path) ?: error("EPUB incompleto: arquivo ausente.")
            require(entry.size <= MAX_ENTRY) { "Capítulo grande demais para importar." }
            val bytes = zip.getInputStream(entry).use { it.readBounded(MAX_ENTRY + 1) }
            consumed += bytes.size
            require(bytes.size <= MAX_ENTRY && consumed <= 64L * 1024 * 1024) { "EPUB excede o limite de texto." }
            return bytes.toString(Charsets.UTF_8)
        }
        fun resolve(base: String, relative: String): String {
            val uri = URI(base).resolve(relative).normalize()
            require(!uri.isAbsolute && !uri.path.startsWith("/") && !uri.path.startsWith("../")) { "Caminho inválido no EPUB." }
            return uri.path
        }
        val container = Jsoup.parse(read("META-INF/container.xml"), "", Parser.xmlParser())
        val packagePath = container.getElementsByTag("rootfile").first()?.attr("full-path")
            ?: error("EPUB sem manifesto.")
        val opf = Jsoup.parse(read(packagePath), "", Parser.xmlParser())
        require(zip.getEntry("META-INF/encryption.xml") == null) { "EPUB protegido ou com fontes criptografadas não é suportado nesta versão." }
        val manifest = opf.getElementsByTag("item").associateBy { it.attr("id") }
        val passages = opf.getElementsByTag("itemref").filter { it.attr("linear") != "no" }.flatMap { ref ->
            val item = manifest[ref.attr("idref")] ?: error("Capítulo ausente no manifesto.")
            val doc = Jsoup.parse(read(resolve(packagePath, item.attr("href"))))
            doc.select("script, style, nav").remove()
            val blocks = doc.select("h1, h2, h3, h4, p, li, blockquote, pre").filter { element ->
                element.parents().none { it.tagName() in setOf("p", "li", "blockquote", "pre") }
            }
            if (blocks.isEmpty()) splitPassages(doc.body().text()) else blocks.flatMap { splitPassages(it.text()) }
        }
        require(passages.isNotEmpty()) { "Nenhum texto legível encontrado no EPUB." }
        ParsedBook(opf.getElementsByTag("dc:title").text().ifBlank { file.nameWithoutExtension },
            opf.getElementsByTag("dc:creator").text().ifBlank { "Autor desconhecido" }, passages, 0)
    }
}

internal fun InputStream.readBounded(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(minOf(limit, 8192))
    var remaining = limit
    while (remaining > 0) {
        val count = read(buffer, 0, minOf(buffer.size, remaining))
        if (count < 0) break
        output.write(buffer, 0, count)
        remaining -= count
    }
    return output.toByteArray()
}
