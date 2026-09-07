package br.com.librumsanctum

import java.text.Normalizer
import java.util.Locale

data class BookNames(val title: String, val author: String)

/** Offline display cleanup. A filename alone cannot identify every title/author boundary. */
object BookNameFormatter {
    private val locale = Locale.forLanguageTag("pt-BR")
    private val smallWords = setOf("a", "o", "as", "os", "um", "uma", "uns", "umas", "e", "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas", "por", "para", "com", "sem", "ao", "aos", "à", "às")
    private val ambiguousEndings = setOf("de", "do", "da", "sobre", "a", "ao", "para", "com")
    private val acronyms = setOf("PDF", "EPUB", "IA", "AI", "USB", "FBI", "CIA", "NASA", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    // A small, explicit catalog, not a claim of universal author recognition.
    private val authors = listOf(
        "Ernest Hemingway", "F. Scott Fitzgerald", "Jane Austen", "Machado de Assis",
        "José de Alencar", "José Saramago", "Clarice Lispector", "George Orwell",
        "Virginia Woolf", "Franz Kafka", "Jorge Amado", "Fernando Pessoa",
        "Agatha Christie", "Arthur Conan Doyle", "J. R. R. Tolkien",
    )
    private val unknownAuthors = setOf("", "autor desconhecido", "desconhecido", "unknown", "unknown author")

    fun resolve(rawTitle: String, rawAuthor: String): BookNames {
        val title = clean(rawTitle).ifBlank { "Livro" }
        val authorText = clean(rawAuthor)
        val explicitAuthor = authorText.takeUnless { it.lowercase(locale) in unknownAuthors }
        val author = explicitAuthor?.let { candidate -> authors.firstOrNull { key(it) == key(candidate) } ?: titleCase(candidate) }
        val words = title.split(' ')
        val candidates = if (author != null) listOf(author) else authors
        for (candidate in candidates) {
            // Match whole suffix tokens, allowing punctuation/spacing variants such as fitz-gerald.
            for (start in 1 until words.size) {
                if (key(words.drop(start).joinToString(" ")) != key(candidate)) continue
                val prefix = words.take(start).dropLastWhile { it in setOf("-", "–", "—", "|") }
                if (prefix.isEmpty() || prefix.last().lowercase(locale) in ambiguousEndings) continue
                val withoutByline = if (prefix.last().equals("por", true)) prefix.dropLast(1) else prefix
                if (withoutByline.isNotEmpty()) return BookNames(titleCase(withoutByline.joinToString(" ")), candidate)
            }
        }
        return BookNames(titleCase(title), author ?: "Autor desconhecido")
    }

    private fun clean(value: String): String {
        val text = value.trim().replace(Regex("(?i)\\.(pdf|epub)$"), "")
        // Preserve literary hyphens in already spaced titles, e.g. O Homem-Aranha.
        val isSlug = !text.any { it.isWhitespace() } && text.count { it == '-' } >= 2
        return (if (isSlug) text.replace('-', ' ') else text).replace('_', ' ')
            .replace(Regex("\\s+"), " ").trim()
    }

    private fun key(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun titleCase(value: String): String {
        val allUppercase = value.any { it.isLetter() } && value.none { it.isLowerCase() }
        return value.split(' ').mapIndexed { index, word ->
            val lower = word.lowercase(locale)
            when {
                index > 0 && lower in smallWords -> lower
                word.uppercase(Locale.ROOT) in acronyms -> word.uppercase(Locale.ROOT)
                !allUppercase && word.any { it.isUpperCase() } -> word
                else -> lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            }
        }.joinToString(" ")
    }
}
