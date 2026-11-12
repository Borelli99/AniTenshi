package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import ca.mpreg.imagedecoder.ImageDecoder
import eu.kanade.presentation.reader.ReaderError
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import kotlinx.coroutines.CancellationException
import okio.Buffer
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import tachiyomi.i18n.tail.TLMR
import kotlin.math.min

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
) {
    val context = LocalContext.current
    val imageUrl = page.imageUrl?.takeIf { it.startsWith("http", ignoreCase = true) }
    ReaderError(
        message = rememberReaderErrorMessage(page, error),
        onRetry = { page.chapter.pageLoader?.retryPage(page) },
        onOpenInWebView = imageUrl?.let {
            { context.startActivity(WebViewActivity.newIntent(context, it, sourceId)) }
        },
        modifier = modifier,
        onButtonPressChange = onButtonPressChange,
    )
}

/**
 * AniTenshi: what the reader says about [page] that failed with [error]: the plain sentence, then the reason when it
 * is known and, for an image that did arrive but couldn't be shown, what the image is. Null for the plain sentence
 * alone.
 */
@Composable
fun rememberReaderErrorMessage(page: ReaderPage?, error: Throwable?): String? {
    val context = LocalContext.current
    val reason = error?.let { with(context) { it.formattedMessage } }
    // Only for an image that was downloaded: before that there is nothing to describe
    val imageNote by produceState<String?>(null, page, error) {
        value = if (page != null && page.status == Page.State.READY) describePageImage(context, page) else null
    }
    val details = listOfNotNull(reason, imageNote)
    if (details.isEmpty()) return null
    return (listOf(context.stringResource(MR.strings.decode_image_error)) + details).joinToString("\n")
}

/**
 * A downloaded page's image in a few words, such as "JPEG 800×24000, 6.2 MB". It tells an image the decoder can't
 * handle from something that isn't an image at all, such as a block page the site sent in its place, whose first
 * characters are added. Null when the page has nothing to read.
 */
private suspend fun describePageImage(context: Context, page: ReaderPage): String? = withIOContext {
    try {
        val openStream = page.stream ?: return@withIOContext null
        val buffer = openStream().use { Buffer().readFrom(it) }
        val size = Formatter.formatShortFileSize(context, buffer.size)
        val image = try {
            ImageDecoder.open(buffer.peek().inputStream()).use { dec -> "${dec.format} ${dec.width}×${dec.height}" }
        } catch (e: Exception) {
            null
        }
        if (image != null) {
            "$image, $size"
        } else {
            val start = buffer.peek().readUtf8(min(buffer.size, NOT_AN_IMAGE_PREVIEW_BYTES))
                .filter { it in ' '..'~' }
                .trim()
            listOf(context.stringResource(TLMR.strings.anitenshi_reader_not_an_image, size), start)
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

private const val NOT_AN_IMAGE_PREVIEW_BYTES = 48L

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
    ) {
        data = Data(page, error, sourceId, onButtonPressChange)
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
            )
        }
    }

    private data class Data(
        val page: ReaderPage,
        val error: Throwable?,
        val sourceId: Long?,
        val onButtonPressChange: ((pressed: Boolean) -> Unit)?,
    )
}
