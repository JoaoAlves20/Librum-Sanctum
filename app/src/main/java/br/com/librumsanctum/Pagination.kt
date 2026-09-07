package br.com.librumsanctum

data class TextAnchor(val passage: Int, val offset: Int = 0) : Comparable<TextAnchor> {
    override fun compareTo(other: TextAnchor): Int =
        compareValuesBy(this, other, TextAnchor::passage, TextAnchor::offset)
}

data class PageFragment(val anchor: TextAnchor, val text: String, val height: Int, val spaceBefore: Int)
data class ReadingPage(val fragments: List<PageFragment>) {
    val anchor: TextAnchor get() = fragments.first().anchor
}

/** Measurements must use the same font, width and line spacing as the reader. */
data class ParagraphMeasurement(val height: Int, val lineEnds: List<Int>, val lineBottoms: List<Float>)

fun paginate(
    passages: List<Passage>, pageHeight: Int, paragraphSpacing: Int,
    measure: (String) -> ParagraphMeasurement,
): List<ReadingPage> {
    require(pageHeight > 0 && paragraphSpacing >= 0)
    val pages = mutableListOf<ReadingPage>()
    var fragments = mutableListOf<PageFragment>()
    var used = 0
    fun flush() {
        if (fragments.isNotEmpty()) pages += ReadingPage(fragments.toList())
        fragments = mutableListOf()
        used = 0
    }
    passages.forEachIndexed { index, passage ->
        var offset = 0
        while (offset < passage.text.length) {
            val spacing = if (fragments.isEmpty()) 0 else paragraphSpacing
            val available = pageHeight - used - spacing
            val remaining = passage.text.substring(offset)
            val layout = measure(remaining)
            var end = remaining.length
            var height = layout.height
            if (height > available) {
                val fittingLines = layout.lineBottoms.count { it <= available }
                end = if (fittingLines == 0) 0 else layout.lineEnds[fittingLines - 1]
                // A new last line can have different font padding; verify the exact fragment.
                var line = fittingLines - 1
                while (end > 0) {
                    height = measure(remaining.substring(0, end)).height
                    if (height <= available) break
                    line--
                    end = if (line < 0) 0 else layout.lineEnds[line]
                }
                if (end == 0) {
                    check(fragments.isNotEmpty()) { "A tela não comporta uma linha. Reduza a fonte ou use a rolagem." }
                    flush()
                    continue
                }
            }
            fragments += PageFragment(TextAnchor(index, offset), remaining.substring(0, end), height, spacing)
            used += spacing + height
            offset += end
            if (offset < passage.text.length) flush()
        }
    }
    flush()
    return pages
}

/** Find the page containing an anchor even after the text has been repaginated. */
fun List<ReadingPage>.pageFor(anchor: TextAnchor): Int {
    if (isEmpty()) return 0
    var low = 0
    var high = lastIndex
    while (low < high) {
        val middle = (low + high + 1) / 2
        if (this[middle].anchor <= anchor) low = middle else high = middle - 1
    }
    return low
}
