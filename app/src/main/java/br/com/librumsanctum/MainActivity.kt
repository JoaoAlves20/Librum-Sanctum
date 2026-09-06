package br.com.librumsanctum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent { LibrumApp() }
    }
}

@Composable fun LibrumApp(model: LibraryViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = when (state.settings.theme) {
        "Escuro" -> darkColorScheme(primary = Color(0xFFD9BB7A), background = Color(0xFF171E1B), surface = Color(0xFF202A25), onSurface = Color(0xFFE9E4D8))
        "Claro" -> lightColorScheme(primary = Color(0xFF315848), background = Color(0xFFFAFBF8), surface = Color.White)
        else -> lightColorScheme(primary = Color(0xFF315848), background = Color(0xFFF4EDDE), surface = Color(0xFFECE2CF), onSurface = Color(0xFF302D25))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(model::import) }
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = colors.background) {
            Box(Modifier.safeDrawingPadding()) {
                if (state.selected == null) LibraryScreen(state, model::open) { picker.launch(arrayOf("application/pdf", "application/epub+zip")) }
                else key(state.selected!!.id) { ReaderScreen(state, model) }
                if (state.busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable(enabled = true) {}, contentAlignment = Alignment.Center) {
                    Card { Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Preparando sua biblioteca…")
                    } }
                }
            }
        }
        state.error?.let { error -> AlertDialog(onDismissRequest = model::clearError, title = { Text("Não foi possível concluir") }, text = { Text(error) }, confirmButton = { TextButton(onClick = model::clearError) { Text("Entendi") } }) }
    }
}

@Composable private fun LibraryScreen(state: LibraryState, open: (Book) -> Unit, import: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val last = state.books.filter { it.lastRead > 0 }.maxByOrNull { it.lastRead }
    val filtered = state.books.filter { it.title.contains(query, true) || it.author.contains(query, true) }.sortedByDescending { it.lastRead }
    LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
      item(span = { GridItemSpan(maxLineSpan) }) { Column {
        Spacer(Modifier.height(24.dp))
        Text("L I B R U M  S A N C T U M", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Seu refúgio de leitura.", fontFamily = FontFamily.Serif, fontSize = 30.sp)
        Text("Seus livros. Seu ritmo. Mesmo offline.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 22.dp))
        if (last != null) Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF243F36), contentColor = Color(0xFFF4EDDE)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("RETOME SUA HISTÓRIA", fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFFD9BB7A))
                Text(last.title, fontFamily = FontFamily.Serif, fontSize = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 10.dp))
                LinearProgressIndicator(progress = { last.progress }, modifier = Modifier.fillMaxWidth(), color = Color(0xFFD9BB7A), trackColor = Color(0xFF4A6155))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(last.progress * 100).toInt()}% lido", fontSize = 12.sp)
                    TextButton(onClick = { open(last) }) { Text("Continuar →", color = Color(0xFFD9BB7A)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Minha biblioteca · ${state.books.size}", fontFamily = FontFamily.Serif, fontSize = 20.sp)
            FilledTonalButton(onClick = import, enabled = !state.busy) { Text("+ Importar") }
        }
        if (state.books.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("❧", fontSize = 64.sp, color = MaterialTheme.colorScheme.primary)
            Text("Uma estante de possibilidades", fontFamily = FontFamily.Serif, fontSize = 23.sp)
            Text("Importe seu primeiro PDF ou EPUB.\nTudo fica guardado neste aparelho.", modifier = Modifier.padding(vertical = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Button(onClick = import, enabled = !state.busy) { Text("Escolher um livro") }
        } else {
            OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Buscar título ou autor") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            if (filtered.isEmpty()) Text("Nenhum livro encontrado.", Modifier.padding(vertical = 24.dp))
        }
      } }
      items(filtered, key = { it.id }) { book -> BookCard(book) { open(book) } }
    }
}

@Composable private fun BookCard(book: Book, onClick: () -> Unit) {
    val palette = listOf(Color(0xFF315848), Color(0xFF794D3C), Color(0xFF46536B), Color(0xFF705E3C))
    val color = palette[(book.id.hashCode() and Int.MAX_VALUE) % palette.size]
    Column(Modifier.clickable(onClick = onClick)) {
        Surface(color = color, contentColor = Color(0xFFF4EDDE), shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp), shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().height(195.dp)) {
                Box(Modifier.width(9.dp).fillMaxHeight().background(Color.Black.copy(alpha = .18f)))
                Column(Modifier.padding(14.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(book.format, fontSize = 10.sp, letterSpacing = 2.sp, color = Color(0xFFD9BB7A))
                    Text(book.title, fontFamily = FontFamily.Serif, fontSize = 21.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Text("—  L S  —", fontSize = 11.sp, color = Color(0xFFD9BB7A))
                }
            }
        }
        Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp))
        Text(book.author, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        Text(if (book.lastRead == 0L) "Ainda não aberto" else "${(book.progress * 100).toInt()}% lido", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable private fun ReaderScreen(state: LibraryState, model: LibraryViewModel) {
    val book = state.selected ?: return
    var preferences by remember { mutableStateOf(false) }
    BackHandler { model.close() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = model::close) { Text("‹ Estante") }
            Text(book.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Serif)
            TextButton(onClick = { preferences = true }) { Text("Aa", fontSize = 22.sp) }
        }
        HorizontalDivider()
        if (book.original) Box(Modifier.weight(1f)) { PdfReader(book, model.repository.source(book)) }
        else key(book.original) {
            val list = rememberLazyListState(initialFirstVisibleItemIndex = book.position)
            LaunchedEffect(list) {
                snapshotFlow { list.firstVisibleItemIndex }.distinctUntilChanged().collect { model.position(it) }
            }
            LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(state.passages.size) { index ->
                    Text(state.passages[index].text, fontFamily = if (state.settings.serif) FontFamily.Serif else FontFamily.SansSerif,
                        fontSize = state.settings.size.sp, lineHeight = (state.settings.size * 1.65f).sp)
                }
                item { Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Você chegou ao fim.", fontFamily = FontFamily.Serif, fontSize = 24.sp)
                    TextButton(onClick = model::finish) { Text(if (book.finished) "Leitura concluída ✓" else "Marcar como concluído") }
                } }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            if (book.original) {
                TextButton(onClick = { model.position(book.pdfPage - 1) }, enabled = book.pdfPage > 0) { Text("←") }
                Text("${book.pdfPage + 1} / ${book.pages}")
                if (book.pdfPage == book.pages - 1) TextButton(onClick = model::finish) { Text(if (book.finished) "✓" else "Concluir") }
                else TextButton(onClick = { model.position(book.pdfPage + 1) }) { Text("→") }
            } else Text("${(book.progress * 100).toInt()}% lido", fontSize = 12.sp)
            if (book.format == "PDF" && book.passages > 0) TextButton(onClick = model::original) { Text(if (book.original) "Texto" else "PDF original") }
        }
    }
    if (preferences) AlertDialog(onDismissRequest = { preferences = false }, title = { Text("Do seu jeito", fontFamily = FontFamily.Serif) }, text = {
        Column {
            Text("Aparência")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Claro", "Sépia", "Escuro").forEach { theme ->
                FilterChip(selected = state.settings.theme == theme, onClick = { model.settings(state.settings.copy(theme = theme)) }, label = { Text(theme) })
            } }
            Spacer(Modifier.height(16.dp)); Text("Tamanho do texto · ${state.settings.size.toInt()}")
            Slider(value = state.settings.size, onValueChange = { model.settings(state.settings.copy(size = it)) }, valueRange = 14f..32f, steps = 8)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Fonte clássica", Modifier.weight(1f)); Switch(checked = state.settings.serif, onCheckedChange = { model.settings(state.settings.copy(serif = it)) }) }
            Text("Fonte e tamanho se aplicam à leitura em texto.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { preferences = false }) { Text("Voltar à leitura") } })
}
