package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.domain.extension.anime.interactor.AddAnimeExtensionStore
import mihon.domain.extension.anime.interactor.GetAnimeExtensionStores
import mihon.domain.extension.manga.interactor.AddMangaExtensionStore
import mihon.domain.extension.manga.interactor.GetMangaExtensionStores
import mihon.domain.extension.model.ExtensionStore

/**
 * AniTenshi: adds the first anime and manga repos from the first-launch screens.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class OnboardingReposViewModel(
    private val addAnimeExtensionStore: AddAnimeExtensionStore,
    private val addMangaExtensionStore: AddMangaExtensionStore,
    getAnimeExtensionStores: GetAnimeExtensionStores,
    getMangaExtensionStores: GetMangaExtensionStores,
    private val animeExtensionManager: AnimeExtensionManager,
    private val mangaExtensionManager: MangaExtensionManager,
) : ViewModel() {

    val animeStores: StateFlow<List<ExtensionStore>> = getAnimeExtensionStores.subscribe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mangaStores: StateFlow<List<ExtensionStore>> = getMangaExtensionStores.subscribe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _anime = MutableStateFlow(AddState())
    val anime: StateFlow<AddState> = _anime.asStateFlow()

    private val _manga = MutableStateFlow(AddState())
    val manga: StateFlow<AddState> = _manga.asStateFlow()

    fun addAnimeRepo(url: String) {
        add(url.trim(), _anime) {
            addAnimeExtensionStore(it).also { result ->
                if (result.isSuccess) animeExtensionManager.findAvailableExtensions()
            }
        }
    }

    fun addMangaRepo(url: String) {
        add(url.trim(), _manga) {
            addMangaExtensionStore(it).also { result ->
                if (result.isSuccess) mangaExtensionManager.findAvailableExtensions()
            }
        }
    }

    private fun add(
        url: String,
        state: MutableStateFlow<AddState>,
        insert: suspend (String) -> Result<Unit>,
    ) {
        if (url.isBlank() || state.value.processing) return
        viewModelScope.launch {
            state.update { AddState(processing = true) }
            insert(url)
                .onSuccess { state.update { AddState() } }
                .onFailure { error ->
                    state.update { AddState(error = error.message ?: error.javaClass.simpleName) }
                }
        }
    }

    @Immutable
    data class AddState(
        val processing: Boolean = false,
        val error: String? = null,
    )
}
