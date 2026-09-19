package br.com.librumsanctum

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

object PdfParser {
    fun parse(file: File, name: String): ParsedBook = PDDocument.load(file).use { pdf ->
        require(!pdf.isEncrypted) { "PDF protegido por senha não é suportado." }
        require(pdf.numberOfPages in 1..3000) { "O PDF deve ter entre 1 e 3.000 páginas." }
        var length = 0
        val text = (1..pdf.numberOfPages).flatMap { page ->
            val content = PDFTextStripper().apply {
                startPage = page; endPage = page; sortByPosition = true
                paragraphStart = "\n\n"; paragraphEnd = "\n\n"
                lineSeparator = "\n"
            }.getText(pdf)
            length += content.length
            require(length <= 16_000_000) { "PDF excede o limite de texto." }
            splitPassages(content, page - 1)
        }
        ParsedBook(pdf.documentInformation.title.orEmpty().ifBlank { name.substringBeforeLast('.') },
            pdf.documentInformation.author.orEmpty().ifBlank { "Autor desconhecido" }, text, pdf.numberOfPages)
    }
}
