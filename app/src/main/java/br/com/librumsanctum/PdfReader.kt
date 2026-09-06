package br.com.librumsanctum

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable fun PdfReader(book: Book, file: File) {
    var bitmap by remember(book.id, book.pdfPage) { mutableStateOf<Bitmap?>(null) }
    var error by remember(book.id, book.pdfPage) { mutableStateOf<String?>(null) }
    var scale by remember(book.id, book.pdfPage) { mutableFloatStateOf(1f) }
    var offset by remember(book.id, book.pdfPage) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(book.id, book.pdfPage) {
        try {
            bitmap = withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer -> renderer.openPage(book.pdfPage).use { page ->
                        val ratio = minOf(2f, 2400f / maxOf(page.width, page.height))
                        Bitmap.createBitmap((page.width * ratio).toInt().coerceAtLeast(1), (page.height * ratio).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
                            it.eraseColor(AndroidColor.WHITE); page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    } }
                }
            }
        } catch (e: Exception) { error = "Não foi possível exibir esta página: ${e.message}" }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val rendered = bitmap
        if (error != null) Text(error!!) else if (rendered == null) CircularProgressIndicator()
        else Image(rendered.asImageBitmap(), contentDescription = "Página ${book.pdfPage + 1} de ${book.title}", contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().pointerInput(book.pdfPage) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val limitX = size.width * (scale - 1) / 2f
                    val limitY = size.height * (scale - 1) / 2f
                    offset = Offset((offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
                }
            }.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y))
    }
}
