package br.com.librumsanctum

import org.junit.Assert.*
import org.junit.Test

class PaginationTest {
    private fun measure(text: String): ParagraphMeasurement {
        val ends = (10 until text.length step 10).toList() + text.length
        return ParagraphMeasurement(ends.size * 20, ends, ends.indices.map { (it + 1) * 20f })
    }
    @Test fun splitsLongPassagesAndPreservesEveryCharacterAndAnchor() {
        val passages = listOf(Passage("ABCDEFGHIJ".repeat(25)), Passage("O último parágrafo."))
        val pages = paginate(passages, 85, 8, ::measure)
        assertTrue(pages.size > 1)
        passages.forEachIndexed { index, passage ->
            val fragments = pages.flatMap { it.fragments }.filter { it.anchor.passage == index }
            assertEquals(passage.text, fragments.joinToString("") { it.text })
            var offset = 0
            fragments.forEach { assertEquals(offset, it.anchor.offset); offset += it.text.length }
        }
        assertTrue(pages.all { page -> page.fragments.sumOf { it.height + it.spaceBefore } <= 85 })
        assertTrue(pages.all { it.fragments.first().spaceBefore == 0 })
    }
    @Test fun exactPageBoundaryDoesNotCreateEmptyPage() {
        val pages = paginate(listOf(Passage("a".repeat(40))), 80, 8, ::measure)
        assertEquals(1, pages.size)
        assertEquals(80, pages.single().fragments.single().height)
    }
    @Test fun paragraphSpacingMovesNextParagraphToNextPage() {
        val pages = paginate(listOf(Passage("a"), Passage("b")), 40, 8, ::measure)
        assertEquals(2, pages.size)
        assertEquals(TextAnchor(1), pages.last().anchor)
    }
    @Test fun repaginationFindsPageContainingSavedCharacter() {
        val passages = listOf(Passage("a".repeat(300)))
        val anchor = TextAnchor(0, 165)
        val pages = paginate(passages, 60, 8, ::measure)
        val page = pages.pageFor(anchor)
        assertTrue(pages[page].anchor <= anchor)
        assertTrue(page == pages.lastIndex || pages[page + 1].anchor > anchor)
        assertEquals(0, pages.pageFor(TextAnchor(0)))
        assertEquals(pages.lastIndex, pages.pageFor(TextAnchor(99)))
    }
    @Test fun emptyTextProducesNoBlankPages() {
        assertTrue(paginate(listOf(Passage("")), 40, 8, ::measure).isEmpty())
    }
    @Test fun impossiblySmallViewportProducesActionableError() {
        val error = assertThrows(IllegalStateException::class.java) { paginate(listOf(Passage("a")), 10, 8, ::measure) }
        assertTrue(error.message!!.contains("rolagem"))
    }
}
