package br.com.librumsanctum

data class Passage(val text: String, val sourcePage: Int = 0)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val format: String,
    val passages: Int,
    val pages: Int,
    val position: Int = 0,
    val pdfPage: Int = 0,
    val original: Boolean = false,
    val lastRead: Long = 0,
    val finished: Boolean = false,
    val positionOffset: Int = 0,
    val textVersion: Int = 0,
) {
    val progress: Float get() = if (finished) 1f else if (original || passages == 0)
        pdfPage.toFloat() / pages.coerceAtLeast(1) else position.toFloat() / passages.coerceAtLeast(1)
}

enum class ReadingMode { SCROLL, PAGED }

data class ReadingSettings(
    val theme: String = "Sépia", val size: Float = 20f, val serif: Boolean = true,
    val mode: ReadingMode = ReadingMode.SCROLL,
)

fun splitPassages(text: String, page: Int = 0): List<Passage> =
    text.replace("\r\n", "\n").replace('\r', '\n').replace('\u00a0', ' ')
        .split(Regex("\n\\s*\n|\n(?=\\s*[—–-]\\s*\\p{L})"))
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotEmpty() }.map { Passage(it, page) }
