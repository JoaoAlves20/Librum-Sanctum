package br.com.librumsanctum

/** Whitespace-independent position: paragraph boundaries no longer count as reading progress. */
fun remapAnchor(old: List<Passage>, updated: List<Passage>, anchor: TextAnchor): TextAnchor {
    if (updated.isEmpty()) return TextAnchor(0)
    val index = anchor.passage.coerceIn(0, old.lastIndex.coerceAtLeast(0))
    val page = old.getOrNull(index)?.sourcePage ?: 0
    // PDF page boundaries prevent accumulated extraction differences from shifting later pages.
    val targetPage = updated.indexOfFirst { it.sourcePage == page }.coerceAtLeast(0)
    var remaining = old.take(index).filter { it.sourcePage == page }.sumOf { p -> p.text.count { !it.isWhitespace() } }
    remaining += old.getOrNull(index)?.text?.take(anchor.offset.coerceAtLeast(0))?.count { !it.isWhitespace() } ?: 0
    for (i in targetPage..updated.lastIndex) {
        if (updated[i].sourcePage != page) return TextAnchor((i - 1).coerceAtLeast(0), updated[(i - 1).coerceAtLeast(0)].text.length)
        val text = updated[i].text
        for (offset in text.indices) {
            if (!text[offset].isWhitespace()) {
                if (remaining == 0) return TextAnchor(i, offset)
                remaining--
            }
        }
    }
    return TextAnchor(updated.lastIndex, updated.last().text.length)
}
