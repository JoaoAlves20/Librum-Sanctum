package br.com.librumsanctum

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryState(
    val books: List<Book> = emptyList(), val selected: Book? = null,
    val passages: List<Passage> = emptyList(), val settings: ReadingSettings = ReadingSettings(),
    val busy: Boolean = false, val error: String? = null,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    val repository = LibraryRepository(application)
    private val mutable = MutableStateFlow(LibraryState(settings = repository.settings()))
    val state = mutable.asStateFlow()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val persistenceDispatcher = Dispatchers.IO.limitedParallelism(1)
    init { work { mutable.value = mutable.value.copy(books = repository.books()) } }
    private fun work(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutable.value = mutable.value.copy(busy = true)
            try { withContext(persistenceDispatcher) { block() } }
            catch (e: Exception) { mutable.value = mutable.value.copy(error = e.message ?: "Não foi possível abrir o livro.") }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun import(uri: Uri) = work {
        repository.import(uri)
        mutable.value = mutable.value.copy(books = repository.books())
    }
    fun open(book: Book) = work {
        val content = repository.passages(book)
        val opened = book.copy(lastRead = System.currentTimeMillis())
        repository.save(opened)
        mutable.value = mutable.value.copy(selected = opened, passages = content, books = repository.books())
    }
    fun close() { mutable.value = mutable.value.copy(selected = null, passages = emptyList()) }
    fun clearError() { mutable.value = mutable.value.copy(error = null) }
    fun settings(value: ReadingSettings) {
        repository.saveSettings(value)
        mutable.value = mutable.value.copy(settings = value)
    }
    fun position(position: Int, offset: Int = 0) {
        val book = mutable.value.selected ?: return
        val updated = if (book.original) book.copy(pdfPage = position.coerceIn(0, (book.pages - 1).coerceAtLeast(0)), finished = false)
            else {
                val index = position.coerceIn(0, (book.passages - 1).coerceAtLeast(0))
                book.copy(position = index, positionOffset = offset.coerceIn(0, mutable.value.passages.getOrNull(index)?.text?.length ?: 0), finished = false)
            }
        if (updated != book) update(updated)
    }
    fun finish() { mutable.value.selected?.let { update(it.copy(finished = true)) } }
    fun original() {
        val book = mutable.value.selected ?: return
        if (book.format != "PDF" || book.passages == 0) return
        val passages = mutable.value.passages
        val updated = if (book.original) book.copy(original = false, positionOffset = 0, position = passages.indexOfFirst { it.sourcePage >= book.pdfPage }.let { if (it < 0) passages.lastIndex else it })
            else book.copy(original = true, pdfPage = passages.getOrNull(book.position)?.sourcePage ?: 0)
        update(updated)
    }
    private fun update(book: Book) {
        mutable.value = mutable.value.copy(selected = book, books = mutable.value.books.map { if (it.id == book.id) book else it })
        // Undispatched ordering is not assumed: one IO queue preserves rapid position updates.
        viewModelScope.launch(persistenceDispatcher) {
            try { repository.save(book) }
            catch (e: Exception) { mutable.value = mutable.value.copy(error = "Não foi possível salvar o progresso: ${e.message}") }
        }
    }
}
