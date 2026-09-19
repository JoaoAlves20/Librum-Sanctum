package br.com.librumsanctum

import android.content.Context
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PaginationLayoutTest {
    @Test fun actualFontMeasurementsFitWithoutLosingTextAcrossScreenAndFontSizes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val text = "Uma história sobre jardins, memória e silêncio. 🌿 A leitura continua, palavra por palavra. ".repeat(12)
        val passages = splitPassages(text)
        for (size in listOf(14, 20, 32)) for (width in listOf(220, 355)) for (height in listOf(120, 580)) {
            val measurer = TextMeasurer(createFontFamilyResolver(context), Density(1f, 1.3f), LayoutDirection.Ltr, 0)
            val style = TextStyle(fontFamily = FontFamily.Serif, fontSize = size.sp, lineHeight = (size * 1.65f).sp)
            val pages = paginate(passages, height, 18) { value ->
                val layout = measurer.measure(value, style, constraints = Constraints(maxWidth = width))
                ParagraphMeasurement(layout.size.height, (0 until layout.lineCount).map { layout.getLineEnd(it) },
                    (0 until layout.lineCount).map { layout.getLineBottom(it) })
            }
            assertEquals(passages.joinToString("") { it.text }, pages.flatMap { it.fragments }.joinToString("") { it.text })
            pages.forEach { page ->
                assertTrue(page.fragments.sumOf { it.height + it.spaceBefore } <= height)
                page.fragments.forEach { fragment ->
                    val layout = measurer.measure(fragment.text, style, constraints = Constraints(maxWidth = width, maxHeight = fragment.height))
                    assertFalse("Texto cortado em $width x $height, fonte $size", layout.hasVisualOverflow)
                }
            }
        }
    }
}
