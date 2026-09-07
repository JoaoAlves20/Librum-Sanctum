package br.com.librumsanctum

import org.junit.Assert.*
import org.junit.Test

class BookNameFormatterTest {
    @Test fun recognizesHemingwayInFilename() {
        assertEquals(BookNames("O Velho e o Mar", "Ernest Hemingway"),
            BookNameFormatter.resolve("o-velho-e-o-mar-ernest-hemingway.pdf", ""))
    }
    @Test fun recognizesFitzgeraldDespiteSplitSurname() {
        assertEquals(BookNames("O Grande Gatsby", "F. Scott Fitzgerald"),
            BookNameFormatter.resolve("o-grande-gatsby-f-scott-fitz-gerald", "Autor desconhecido"))
    }
    @Test fun capitalizesWithoutInventingAuthor() {
        assertEquals(BookNames("Orgulho e Preconceito", "Autor desconhecido"),
            BookNameFormatter.resolve("orgulho e preconceito", ""))
    }
    @Test fun usesMetadataAuthorToSplitNamesOutsideCatalog() {
        assertEquals(BookNames("O Jardim Secreto", "Luiza Silva"),
            BookNameFormatter.resolve("o-jardim-secreto-luiza-silva", "luiza silva"))
    }
    @Test fun explicitMetadataAuthorWinsOverFilenameGuess() {
        assertEquals(BookNames("O Velho e o Mar Ernest Hemingway", "Maria Silva"),
            BookNameFormatter.resolve("o-velho-e-o-mar-ernest-hemingway", "Maria Silva"))
    }
    @Test fun leavesAmbiguousAuthorSuffixInTitle() {
        assertEquals(BookNames("A Biografia de Ernest Hemingway", "Autor desconhecido"),
            BookNameFormatter.resolve("a-biografia-de-ernest-hemingway", ""))
        assertEquals(BookNames("Uma Viagem Maria Silva", "Autor desconhecido"),
            BookNameFormatter.resolve("uma-viagem-maria-silva", ""))
    }
    @Test fun handlesAccentsUppercaseUnderscoresAndInitials() {
        assertEquals(BookNames("Memórias Póstumas de Brás Cubas", "Machado de Assis"),
            BookNameFormatter.resolve("MEMÓRIAS_PÓSTUMAS_DE_BRÁS_CUBAS", "MACHADO DE ASSIS"))
        assertEquals(BookNames("Guia do EPUB II", "F. Scott Fitzgerald"),
            BookNameFormatter.resolve("guia do EPUB II", "f scott fitzgerald"))
    }
    @Test fun preservesLiteraryHyphensAndIntentionalMixedCase() {
        assertEquals("O Homem-Aranha e o iPhone", BookNameFormatter.resolve("O Homem-Aranha e o iPhone", "").title)
    }
    @Test fun isIdempotentForExistingLibrary() {
        val once = BookNameFormatter.resolve("o-grande-gatsby-f-scott-fitz-gerald", "")
        assertEquals(once, BookNameFormatter.resolve(once.title, once.author))
        assertEquals("Ernest Hemingway", BookNameFormatter.resolve("Ernest Hemingway", "").title)
    }
}
