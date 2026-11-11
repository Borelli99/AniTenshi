package eu.kanade.tachiyomi.ui.entries

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.extension.InstallStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AniTenshi: what an anime or manga screen knows about its title when the extension it comes from isn't
 * installed (after restoring a backup or syncing on a new device, typically).
 */
@Immutable
data class MissingExtensionState(
    /** The source's name as far as it is known (from the backup, or from the repos); blank when it isn't. */
    val sourceName: String,
    /** The extension in the person's repos that provides the source; null when none was found, or not yet. */
    val extensionName: String? = null,
    /** The repos' lists were read and no extension in them provides the source. */
    val notInRepos: Boolean = false,
    /** Where the install started from this screen is at; [InstallStep.Idle] when none was. */
    val installStep: InstallStep = InstallStep.Idle,
) {
    val isInstalling: Boolean
        get() = !installStep.isCompleted()

    val installFailed: Boolean
        get() = installStep is InstallStep.Error

    /**
     * The extension was installed from here and its source still isn't there: it wasn't loaded (not trusted,
     * hidden by a content filter...). The extension list says why and what to do.
     */
    val installedNotLoaded: Boolean
        get() = installStep == InstallStep.Installed

    /** The extension can be installed from here. */
    val canInstall: Boolean
        get() = extensionName != null && !installedNotLoaded

    /** The name to call the extension by; blank when nothing is known about it. */
    val displayName: String
        get() = extensionName ?: sourceName

    /**
     * What to type in the extension list to find the extension. The list shows what is still to install by
     * source, and what is installed by extension.
     */
    fun searchQuery(sourceId: Long): String {
        return extensionName?.takeIf { installedNotLoaded } ?: sourceName.ifBlank { sourceId.toString() }
    }
}

/**
 * AniTenshi: follows a title whose extension isn't installed. It looks for the extension that provides the source
 * in the person's repos, installs it on request, and tells when the source is finally there.
 *
 * Anime and manga have their own extension and source managers, with the same shape but different types, so
 * everything it needs from them comes in as functions and [E] is the type of an available extension.
 */
class MissingExtensionFinder<E : Any>(
    private val scope: CoroutineScope,
    /** Emits once the installed sources are known, and again each time they change. */
    private val installedSourcesChanges: Flow<*>,
    private val isSourceInstalled: suspend (sourceId: Long) -> Boolean,
    private val availableExtensions: StateFlow<List<E>>,
    /** Reads the repos' lists again, without telling the person when it can't. */
    private val loadAvailableExtensions: suspend () -> Unit,
    /** Whether the person's settings let [E] be offered (its repo isn't switched off, its content is allowed). */
    private val isOffered: (E) -> Boolean,
    /** The name of source [sourceId] in [extension], or null when the extension doesn't provide it. */
    private val sourceNameIn: (extension: E, sourceId: Long) -> String?,
    private val extensionName: (E) -> String,
    private val installExtension: (E) -> Flow<InstallStep>,
    /**
     * The source is installed: now, after having been missing, or all along (it only looked missing because the
     * extensions hadn't loaded yet when [watch] was called). Either way what was shown in its place is stale.
     */
    private val onSourceInstalled: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow<MissingExtensionState?>(null)

    /** Null while the source is installed (or before [watch]). */
    val state: StateFlow<MissingExtensionState?> = mutableState.asStateFlow()

    @Volatile
    private var found: E? = null
    private var watchJob: Job? = null
    private var installJob: Job? = null

    /**
     * Starts following source [sourceId], which came out as not installed and is known as [knownName] (blank when
     * unknown).
     */
    fun watch(sourceId: Long, knownName: String) {
        if (watchJob != null) return
        watchJob = scope.launch(Dispatchers.IO) {
            installedSourcesChanges
                .map { isSourceInstalled(sourceId) }
                .distinctUntilChanged()
                .collectLatest { installed ->
                    if (installed) {
                        found = null
                        mutableState.value = null
                        onSourceInstalled()
                    } else {
                        mutableState.value = MissingExtensionState(sourceName = knownName)
                        lookInTheRepos(sourceId, knownName)
                    }
                }
        }
    }

    private suspend fun lookInTheRepos(sourceId: Long, knownName: String): Unit = coroutineScope {
        launch {
            // The repos' lists are normally read when the extension page is opened. Give what is already in
            // memory a moment to arrive; if there is nothing, read them now.
            delay(AVAILABLE_LIST_WAIT_MS)
            if (availableExtensions.value.isEmpty()) loadAvailableExtensions()
        }
        availableExtensions.collect { extensions ->
            val match = extensions.firstNotNullOfOrNull { extension ->
                if (!isOffered(extension)) return@firstNotNullOfOrNull null
                sourceNameIn(extension, sourceId)?.let { name -> extension to name }
            }
            found = match?.first
            mutableState.update { current ->
                current?.copy(
                    sourceName = match?.second?.takeIf { it.isNotBlank() } ?: knownName,
                    extensionName = match?.first?.let(extensionName),
                    notInRepos = match == null && extensions.isNotEmpty(),
                )
            }
        }
    }

    /** Installs the extension found in the repos. Nothing when there is none, or while one is being installed. */
    fun install() {
        val extension = found ?: return
        if (installJob?.isActive == true) return
        installJob = scope.launch(Dispatchers.IO) {
            var last: InstallStep = InstallStep.Pending
            mutableState.update { it?.copy(installStep = last) }
            installExtension(extension)
                .onEach { step ->
                    last = step
                    if (step != InstallStep.Installed) mutableState.update { it?.copy(installStep = step) }
                }
                // The flow never ends by itself: it stays on its last step
                .takeWhile { step -> !step.isCompleted() }
                .collect()
            if (last == InstallStep.Installed) {
                // The source shows up a moment after the package is installed, and the state goes back to null.
                // If it is still there after the wait, the extension was installed but not loaded.
                delay(SOURCE_LOAD_WAIT_MS)
                mutableState.update { it?.copy(installStep = InstallStep.Installed) }
            }
        }
    }

    private companion object {
        const val AVAILABLE_LIST_WAIT_MS = 500L
        const val SOURCE_LOAD_WAIT_MS = 4000L
    }
}
