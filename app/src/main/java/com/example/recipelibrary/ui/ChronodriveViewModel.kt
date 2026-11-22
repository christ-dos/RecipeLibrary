package com.example.recipelibrary.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.recipelibrary.data.chronodrive.ChoiceWithProduct
import com.example.recipelibrary.data.chronodrive.ChronodriveProduct
import com.example.recipelibrary.data.chronodrive.ChronodriveRepository
import com.example.recipelibrary.data.chronodrive.ChronodriveResolution
import com.example.recipelibrary.data.chronodrive.FavoritesFileParser
import com.example.recipelibrary.data.chronodrive.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Favoris Chronodrive et choix d'articles (spec 003). */
class ChronodriveViewModel(private val repo: ChronodriveRepository) : ViewModel() {

    val products: StateFlow<List<ChronodriveProduct>> =
        repo.products.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val choices: StateFlow<List<ChoiceWithProduct>> =
        repo.choices.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Lit le fichier choisi (UTF-8, 1 Mo au plus) puis l'importe ; toute erreur : rien de modifié. */
    fun import(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        val text = withContext(Dispatchers.IO) {
            runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readNBytesCompat(FavoritesFileParser.MAX_BYTES + 1)
                    if (bytes.size > FavoritesFileParser.MAX_BYTES) null else String(bytes, Charsets.UTF_8)
                }
            }.getOrNull()
        }
        _message.value = when (val r = text?.let { repo.importFavorites(it) } ?: ImportResult.Invalid) {
            is ImportResult.Done -> r.report.message
            ImportResult.Invalid -> INVALID
        }
    }

    suspend fun resolveChrono(itemKey: String, term: String): ChronodriveResolution = repo.resolveChrono(itemKey, term)

    fun setChoice(itemKey: String, productId: Long) = viewModelScope.launch { repo.setChoice(itemKey, productId) }

    fun forgetChoice(itemKey: String) = viewModelScope.launch { repo.forgetChoice(itemKey) }

    class Factory(private val repo: ChronodriveRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChronodriveViewModel(repo) as T
    }

    companion object {
        const val INVALID = "Ce fichier ne contient aucun favori Chronodrive reconnu"
    }
}

/** Lit au plus [max] octets (InputStream.readNBytes n'existe qu'à partir d'Android 13). */
private fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (out.size() < max) {
        val n = read(buf, 0, minOf(buf.size, max - out.size()))
        if (n < 0) break
        out.write(buf, 0, n)
    }
    return out.toByteArray()
}
