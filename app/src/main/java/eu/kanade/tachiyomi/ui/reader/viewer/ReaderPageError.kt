package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.BitmapFactory
import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import ca.mpreg.imagedecoder.ImageDecoder
import eu.kanade.presentation.reader.ReaderError
import eu.kanade.presentation.reader.ReaderErrorSuggestion
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.data.coil.PageTooLargeException
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import kotlinx.coroutines.CancellationException
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import tachiyomi.presentation.core.i18n.stringResource
import java.io.FilterInputStream
import java.io.InputStream

/**
 * [ReaderError] for a [page] that failed with [error]: Retry asks its loader again, and an image from the web can
 * be opened in a WebView with the cookies of the source [sourceId].
 */
@Composable
fun ReaderPageError(
    page: ReaderPage,
    error: Throwable?,
    sourceId: Long?,
    modifier: Modifier = Modifier,
    onButtonPressChange: ((pressed: Boolean) -> Unit)? = null,
    hint: ReaderErrorHint? = null,
) {
    val context = LocalContext.current
    val imageUrl = page.imageUrl?.takeIf { it.startsWith("http", ignoreCase = true) }
    val details = rememberReaderErrorDetails(page, error)
    ReaderError(
        details = details.text,
        onRetry = { page.chapter.pageLoader?.retryPage(page) },
        onOpenInWebView = imageUrl?.let {
            { context.startActivity(WebViewActivity.newIntent(context, it, sourceId)) }
        },
        modifier = modifier,
        onButtonPressChange = onButtonPressChange,
        suggestion = readerErrorSuggestion(hint, details),
    )
}

/**
 * AniTenshi: what to suggest for an image the bundled decoder refuses and Android's own reads. Only the long strip
 * falls back to Android's (PlatformStripDecoder); the other viewers can't show such an image.
 */
sealed interface ReaderErrorHint {
    /** From a paged viewer: [onSwitch] sets the manga's reading mode to long strip. */
    class UseLongStrip(val onSwitch: () -> Unit) : ReaderErrorHint

    /** From the high quality renderer, in any reading mode: only turning it off helps. */
    data object TurnOffHighQuality : ReaderErrorHint
}

/**
 * AniTenshi: what the reader says under "The image couldn't be loaded".
 *
 * @param text the reason when it is known and, for an image that did arrive but couldn't be shown, what the image
 * is; null when there is nothing to add.
 * @param onlyAndroidReads the image is one the bundled decoder refuses and Android's own reads.
 */
@Immutable
class ReaderErrorDetails(val text: String?, val onlyAndroidReads: Boolean)

/** AniTenshi: [ReaderErrorDetails] for [page] that failed with [error]. */
@Composable
fun rememberReaderErrorDetails(page: ReaderPage?, error: Throwable?): ReaderErrorDetails {
    val context = LocalContext.current
    val reason = when (error) {
        null -> null
        is PageTooLargeException -> context.stringResource(TLMR.strings.anitenshi_reader_image_too_large)
        else -> with(context) { error.formattedMessage }
    }
    // Only for an image that was downloaded: before that there is nothing to describe
    val imageNote by produceState<ImageNote?>(null, page, error) {
        value = if (page != null && page.status == Page.State.READY) describePageImage(context, page) else null
    }
    val lines = listOfNotNull(reason, imageNote?.text)
    return ReaderErrorDetails(
        text = lines.takeIf { it.isNotEmpty() }?.joinToString("\n"),
        onlyAndroidReads = imageNote?.onlyAndroidReads == true,
    )
}

/**
 * AniTenshi: the card under the message, for an image only Android reads: in a paged viewer, a button that moves
 * the manga to long strip; in the high quality renderer, where turning it off is the only way, just that advice.
 */
@Composable
fun readerErrorSuggestion(hint: ReaderErrorHint?, details: ReaderErrorDetails): ReaderErrorSuggestion? {
    if (!details.onlyAndroidReads) return null
    return when (hint) {
        is ReaderErrorHint.UseLongStrip -> {
            val longStrip = stringResource(MR.strings.webtoon_viewer)
            ReaderErrorSuggestion(
                icon = ImageVector.vectorResource(ReadingMode.WEBTOON.iconRes),
                title = stringResource(TLMR.strings.anitenshi_reader_long_strip_only_title),
                body = stringResource(TLMR.strings.anitenshi_reader_long_strip_only_body, longStrip),
                actionLabel = stringResource(TLMR.strings.anitenshi_reader_switch_to_long_strip, longStrip),
                onAction = hint.onSwitch,
            )
        }
        ReaderErrorHint.TurnOffHighQuality -> ReaderErrorSuggestion(
            icon = Icons.Outlined.Tune,
            title = stringResource(TLMR.strings.anitenshi_reader_high_quality_only_title),
            body = stringResource(
                TLMR.strings.anitenshi_reader_high_quality_only_body,
                stringResource(MR.strings.pref_high_quality_renderer),
            ),
        )
        null -> null
    }
}

/**
 * A downloaded page's image in a few words, such as "JPEG 800×24000, 6.2 MB".
 *
 * @param onlyAndroidReads the bundled decoder refuses the image and Android's own reads it.
 */
private class ImageNote(val text: String, val onlyAndroidReads: Boolean)

/**
 * Describes the image of a downloaded [page]. It tells an image the decoder can't handle from something that isn't
 * an image at all, such as a block page the site sent in its place, whose first characters are added. Null when the
 * page has nothing to read.
 *
 * Only the start of the image is read, each time from a stream of its own. The whole of it can be tens of MB, and a
 * failed chapter has one of these for every page in reach: read whole, they filled the memory and froze the reader.
 */
private suspend fun describePageImage(context: Context, page: ReaderPage): ImageNote? = withIOContext {
    try {
        val openStream = page.stream ?: return@withIOContext null
        val bytes = openStream().use { it.available() }
        val size = if (bytes > 0) Formatter.formatShortFileSize(context, bytes.toLong()) else null

        val bundled = try {
            openStream().use { stream ->
                // available() 0 stops the bundled decoder's read after the header
                val headerOnly = object : FilterInputStream(stream) {
                    override fun available() = 0
                }
                ImageDecoder.open(headerOnly, complete = false).use { dec ->
                    "${dec.format} ${dec.width}×${dec.height}"
                }
            }
        } catch (e: Exception) {
            null
        }
        // The bundled decoder refuses some images Android reads, such as a PNG over 32,767 pixels a side
        val image = bundled ?: openStream().use { describeWithPlatform(it) }

        if (image != null) {
            ImageNote(listOfNotNull(image, size).joinToString(), onlyAndroidReads = bundled == null)
        } else {
            val head = ByteArray(NOT_AN_IMAGE_PREVIEW_BYTES)
            val read = openStream().use { it.read(head) }.coerceAtLeast(0)
            val start = String(head, 0, read, Charsets.ISO_8859_1).filter { it in ' '..'~' }.trim()
            val notAnImage = context.stringResource(TLMR.strings.anitenshi_reader_not_an_image, size ?: "?")
            ImageNote(listOf(notAnImage, start).filter { it.isNotEmpty() }.joinToString("\n"), onlyAndroidReads = false)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

/** Format and size as Android's own decoder reads them, or null when it can't read the image either. */
private fun describeWithPlatform(stream: InputStream): String? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeStream(stream, null, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    val format = options.outMimeType?.substringAfter('/')?.uppercase().orEmpty()
    return "$format ${options.outWidth}×${options.outHeight}".trim()
}

private const val NOT_AN_IMAGE_PREVIEW_BYTES = 48

/**
 * [ReaderPageError] for the view-based viewers, filling the view, in the theme of its reader-themed [context].
 */
class ReaderPageErrorView(context: Context) : AbstractComposeView(context) {

    private var data by mutableStateOf<Data?>(null)

    fun bind(
        page: ReaderPage,
        error: Throwable?,
        sourceId: Long?,
        onButtonPressChange: ((pressed: Boolean) -> Unit)? = null,
        hint: ReaderErrorHint? = null,
    ) {
        data = Data(page, error, sourceId, onButtonPressChange, hint)
    }

    @Composable
    override fun Content() {
        val data = data ?: return
        ReaderThemedContent(context) {
            ReaderPageError(
                page = data.page,
                error = data.error,
                sourceId = data.sourceId,
                modifier = Modifier.fillMaxSize(),
                onButtonPressChange = data.onButtonPressChange,
                hint = data.hint,
            )
        }
    }

    private data class Data(
        val page: ReaderPage,
        val error: Throwable?,
        val sourceId: Long?,
        val onButtonPressChange: ((pressed: Boolean) -> Unit)?,
        val hint: ReaderErrorHint?,
    )
}
