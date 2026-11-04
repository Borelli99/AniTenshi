package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.reader.LocalLastChapterNumber
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [content] in the app's theme under the reader's own night mode, from [readerThemedContext].
 *
 * Pages sit on the reader background, so a light app over a black reader would otherwise draw dark text on black.
 * [TachiyomiTheme] reads that from the configuration, which the view-based viewers got by inflating their views
 * from a reader-themed context.
 */
@Composable
fun ReaderThemedContent(readerThemedContext: Context, content: @Composable () -> Unit) {
    // AniTenshi: the manga's last chapter number, for "Capitolo 296 / 300" on the pages between chapters
    val lastChapterFlow = remember(readerThemedContext) {
        readerThemedContext.readerActivity()?.viewModel?.state
            ?.map { it.lastChapterNumber }
            ?.distinctUntilChanged()
    }
    val lastChapterNumber = lastChapterFlow?.collectAsState(initial = null)?.value
    CompositionLocalProvider(
        LocalContext provides readerThemedContext,
        LocalConfiguration provides readerThemedContext.resources.configuration,
        LocalLastChapterNumber provides lastChapterNumber,
    ) {
        TachiyomiTheme(content = content)
    }
}

/** The [ReaderActivity] behind a (theme-wrapped) reader context, if any. */
private fun Context.readerActivity(): ReaderActivity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<ReaderActivity>()
        .firstOrNull()
