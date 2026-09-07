package br.com.librumsanctum

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun TextReader(state: LibraryState, model: LibraryViewModel, modifier: Modifier = Modifier) {
    val book = state.selected ?: return
    val settings = state.settings
    val style = LocalTextStyle.current.copy(
        fontFamily = if (settings.serif) FontFamily.Serif else FontFamily.SansSerif,
        fontSize = settings.size.sp, lineHeight = (settings.size * 1.65f).sp,
    )
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    // Recreate layout state after a geometry change, starting from the saved text anchor.
    key(book.id, settings.mode, settings.size, settings.serif, density.density, density.fontScale, direction) {
        if (settings.mode == ReadingMode.PAGED) PagedTextReader(state, model, style, modifier)
        else ScrollingTextReader(state, model, style, modifier)
    }
}

@Composable private fun ScrollingTextReader(
    state: LibraryState, model: LibraryViewModel, style: TextStyle, modifier: Modifier,
) {
    val book = state.selected ?: return
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
        val width = constraints.maxWidth
        key(width) {
            val index = book.position.coerceIn(0, state.passages.lastIndex.coerceAtLeast(0))
            val initialOffset = remember {
                state.passages.getOrNull(index)?.text?.let { text ->
                    val layout = measurer.measure(text, style, constraints = Constraints(maxWidth = width))
                    val line = layout.getLineForOffset(book.positionOffset.coerceIn(0, text.length))
                    layout.getLineTop(line).toInt().coerceAtLeast(0)
                } ?: 0
            }
            val list = rememberLazyListState(index, initialOffset)
            LaunchedEffect(list) {
                var wasScrolling = false
                var previous: Pair<Int, Int>? = null
                snapshotFlow { Triple(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset, list.isScrollInProgress) }
                    .distinctUntilChanged().collect { (passage, pixels, scrolling) ->
                        val location = passage to pixels
                        if ((scrolling || wasScrolling) && previous != location) state.passages.getOrNull(passage)?.let {
                            val layout = measurer.measure(it.text, style, constraints = Constraints(maxWidth = width))
                            val line = layout.getLineForVerticalPosition(pixels.toFloat())
                            model.position(passage, layout.getLineStart(line))
                        }
                        previous = location
                        wasScrolling = scrolling
                    }
            }
            LazyColumn(
                state = list, modifier = Modifier.fillMaxSize().testTag("scroll-reader"),
                contentPadding = PaddingValues(vertical = 26.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(state.passages.size) { passage -> Text(state.passages[passage].text, style = style) }
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Você chegou ao fim.", fontFamily = FontFamily.Serif, fontSize = 24.sp)
                        TextButton(onClick = model::finish) { Text(if (book.finished) "Leitura concluída ✓" else "Marcar como concluído") }
                    }
                }
            }
        }
    }
}

private data class PaginationResult(val pages: List<ReadingPage> = emptyList(), val error: String? = null)

@Composable private fun PagedTextReader(
    state: LibraryState, model: LibraryViewModel, style: TextStyle, modifier: Modifier,
) {
    val book = state.selected ?: return
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
            val width = (constraints.maxWidth - with(density) { 2 * 28.dp.roundToPx() }).coerceAtLeast(1)
            val height = (constraints.maxHeight - with(density) { 2 * 26.dp.roundToPx() + 48.dp.roundToPx() }).coerceAtLeast(1)
            val spacing = with(density) { 18.dp.roundToPx() }
            key(width, height) {
                // Each pagination job owns its uncached measurer.
                val measurer = rememberTextMeasurer(cacheSize = 0)
                val result by produceState<PaginationResult?>(null, state.passages, width, height) {
                    value = try {
                        withContext(Dispatchers.Default) {
                            PaginationResult(paginate(state.passages, height, spacing) { text ->
                                ensureActive()
                                val layout = measurer.measure(text, style, constraints = Constraints(maxWidth = width))
                                ParagraphMeasurement(layout.size.height,
                                    (0 until layout.lineCount).map { layout.getLineEnd(it) },
                                    (0 until layout.lineCount).map { layout.getLineBottom(it) })
                            })
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        PaginationResult(error = error.message ?: "Não foi possível preparar as páginas.")
                    }
                }
                val pagination = result
                if (pagination == null) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Preparando as páginas…", Modifier.padding(top = 16.dp))
                    }
                } else if (pagination.error != null || pagination.pages.isEmpty()) {
                    Column(Modifier.align(Alignment.Center)) {
                        Text(pagination.error ?: "Não há texto para paginar.")
                        TextButton(onClick = { model.settings(state.settings.copy(mode = ReadingMode.SCROLL)) }) { Text("Usar rolagem") }
                    }
                } else {
                    val pages = pagination.pages
                    val pager = rememberPagerState(initialPage = pages.pageFor(TextAnchor(book.position, book.positionOffset))) { pages.size }
                    val scope = rememberCoroutineScope()
                    LaunchedEffect(pager) {
                        snapshotFlow { pager.settledPage }.distinctUntilChanged().drop(1).collect { page ->
                            val anchor = pages[page].anchor
                            model.position(anchor.passage, anchor.offset)
                        }
                    }
                    Column(Modifier.fillMaxSize()) {
                    HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp, vertical = 26.dp).testTag("paged-reader")) { page ->
                        Column(Modifier.fillMaxSize().testTag("text-page-$page")) {
                            pages[page].fragments.forEach { fragment ->
                                if (fragment.spaceBefore > 0) Spacer(Modifier.height(with(density) { fragment.spaceBefore.toDp() }))
                                Text(fragment.text, style = style,
                                    modifier = Modifier.fillMaxWidth().height(with(density) { fragment.height.toDp() }))
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            enabled = pager.currentPage > 0,
                            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                            modifier = Modifier.semantics { contentDescription = "Página anterior" },
                        ) { Text("←") }
                        Text("Página ${pager.currentPage + 1} de ${pages.size}", Modifier.weight(1f).testTag("page-counter"),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                        val last = pager.currentPage == pages.lastIndex
                        TextButton(
                            onClick = { if (last) model.finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                            modifier = Modifier.semantics { contentDescription = if (last) "Concluir leitura" else "Próxima página" },
                        ) { Text(if (last) "✓" else "→") }
                    }
                    }
                }
            }
    }
}
