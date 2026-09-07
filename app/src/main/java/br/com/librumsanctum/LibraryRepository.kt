package br.com.librumsanctum

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class LibraryRepository(private val context: Context) {
    private val root = File(context.filesDir, "library").apply { mkdirs() }
    private val index = AtomicFile(File(root, "index.json"))
    private val preferences = context.getSharedPreferences("reader", Context.MODE_PRIVATE)
    fun source(book: Book) = File(root, "${book.id}.${book.format.lowercase()}")
    @Synchronized fun books(): List<Book> {
        if (!index.baseFile.exists() && !File(root, "index.json.bak").exists()) return emptyList()
        val array = JSONArray(index.openRead().bufferedReader().use { it.readText() })
        return (0 until array.length()).map { i -> array.getJSONObject(i).let {
            Book(it.getString("id"), it.getString("title"), it.getString("author"), it.getString("format"),
                it.getInt("passages"), it.getInt("pages"), it.optInt("position"), it.optInt("pdfPage"),
                it.optBoolean("original"), it.optLong("lastRead"), it.optBoolean("finished"), it.optInt("positionOffset"))
        } }
    }
    @Synchronized fun save(book: Book) {
        val all = books().filterNot { it.id == book.id } + book
        val array = JSONArray()
        all.forEach { b -> array.put(JSONObject().apply {
            put("id", b.id); put("title", b.title); put("author", b.author); put("format", b.format)
            put("passages", b.passages); put("pages", b.pages); put("position", b.position)
            put("pdfPage", b.pdfPage); put("original", b.original); put("lastRead", b.lastRead); put("finished", b.finished)
            put("positionOffset", b.positionOffset)
        }) }
        val stream = index.startWrite()
        try { stream.write(array.toString().toByteArray()); index.finishWrite(stream) }
        catch (e: Exception) { index.failWrite(stream); throw e }
    }
    fun passages(book: Book): List<Passage> {
        val array = JSONArray(File(root, "${book.id}.json").readText())
        return (0 until array.length()).map { array.getJSONObject(it).let { p -> Passage(p.getString("text"), p.getInt("page")) } }
    }
    fun settings() = ReadingSettings(
        preferences.getString("theme", "Sépia")!!, preferences.getFloat("size", 20f), preferences.getBoolean("serif", true),
        ReadingMode.entries.firstOrNull { it.name == preferences.getString("mode", null) } ?: ReadingMode.SCROLL,
    )
    fun saveSettings(value: ReadingSettings) {
        preferences.edit().putString("theme", value.theme).putFloat("size", value.size)
            .putBoolean("serif", value.serif).putString("mode", value.mode.name).apply()
    }
    fun import(uri: Uri): Book {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "Livro"
        val temp = File.createTempFile("import-", ".tmp", root)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { output ->
                val buffer = ByteArray(8192); var total = 0L
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    total += count; require(total <= 100L * 1024 * 1024) { "O limite por livro é 100 MB." }
                    output.write(buffer, 0, count); digest.update(buffer, 0, count)
                }
            } } ?: error("Não foi possível abrir o arquivo.")
            val id = digest.digest().joinToString("") { "%02x".format(it) }
            books().firstOrNull { it.id == id }?.let { return it }
            val header = temp.inputStream().use { it.readBounded(5) }.toString(Charsets.US_ASCII)
            val format = if (header == "%PDF-") "PDF" else if (header.startsWith("PK")) "EPUB" else error("Selecione um PDF ou EPUB válido.")
            val parsed = if (format == "EPUB") EpubParser.parse(temp) else PDDocument.load(temp).use { pdf ->
                require(!pdf.isEncrypted) { "PDF protegido por senha não é suportado." }
                require(pdf.numberOfPages in 1..3000) { "O PDF deve ter entre 1 e 3.000 páginas." }
                var length = 0
                val text = (1..pdf.numberOfPages).flatMap { page ->
                    val content = PDFTextStripper().apply { startPage = page; endPage = page; sortByPosition = true }.getText(pdf)
                    length += content.length; require(length <= 16_000_000) { "PDF excede o limite de texto." }
                    splitPassages(content, page - 1)
                }
                ParsedBook(pdf.documentInformation.title.orEmpty().ifBlank { name.substringBeforeLast('.') },
                    pdf.documentInformation.author.orEmpty().ifBlank { "Autor desconhecido" }, text, pdf.numberOfPages)
            }
            val book = Book(id, parsed.title.ifBlank { name.substringBeforeLast('.') }, parsed.author, format, parsed.passages.size, parsed.pages, original = parsed.passages.isEmpty())
            temp.copyTo(source(book), overwrite = true)
            File(root, "$id.json").writeText(JSONArray().apply { parsed.passages.forEach { put(JSONObject().put("text", it.text).put("page", it.sourcePage)) } }.toString())
            save(book)
            return book
        } finally { temp.delete() }
    }
}
