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
) {
    val progress: Float get() = if (finished) 1f else if (original || passages == 0)
        pdfPage.toFloat() / pages.coerceAtLeast(1) else position.toFloat() / passages.coerceAtLeast(1)
}

data class ReadingSettings(val theme: String = "Sépia", val size: Float = 20f, val serif: Boolean = true)

fun splitPassages(text: String, page: Int = 0): List<Passage> =
    text.replace("\r", "").split(Regex("\n\\s*\n"))
        .flatMap { paragraph ->
            val words = paragraph.replace(Regex("\\s+"), " ").trim().split(' ').filter { it.isNotBlank() }
            words.chunked(65).map { Passage(it.joinToString(" "), page) }
        }
