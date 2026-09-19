package br.com.librumsanctum

import org.junit.Assert.assertEquals
import org.junit.Test

class TextMigrationTest {
    @Test fun keepsTheSameCharacterAfterParagraphsAreSeparated() {
        val old = listOf(Passage("Primeiro. — Olá! — Tudo bem?"))
        val updated = listOf(Passage("Primeiro."), Passage("— Olá!"), Passage("— Tudo bem?"))
        assertEquals(TextAnchor(2, 2), remapAnchor(old, updated, TextAnchor(0, old[0].text.indexOf("Tudo"))))
    }
    @Test fun joinsArtificialChunksWithoutLosingOffset() {
        assertEquals(TextAnchor(0, 5), remapAnchor(listOf(Passage("Uma"), Passage("frase.")), listOf(Passage("Uma frase.")), TextAnchor(1, 1)))
    }
    @Test fun migrationIsLocalToTheOriginalPdfPage() {
        assertEquals(TextAnchor(1, 2), remapAnchor(listOf(Passage("Antigo", 0), Passage("— Olá", 1)),
            listOf(Passage("Texto diferente na página anterior", 0), Passage("— Olá", 1)), TextAnchor(1, 2)))
    }
}
