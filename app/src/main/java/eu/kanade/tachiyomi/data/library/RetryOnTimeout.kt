package eu.kanade.tachiyomi.data.library

import kotlinx.coroutines.delay
import java.io.InterruptedIOException

/** Pause before asking a site again after it didn't answer in time. */
private const val TIMEOUT_RETRY_DELAY_MS = 5_000L

/**
 * AniTenshi: runs [block] once more, after a short pause, when the site didn't answer in time
 * (OkHttp reports it as an [InterruptedIOException] "timeout"). Slow sites often answer the second
 * time, so a library update reports far fewer "timeout" errors. Other errors are not retried.
 */
internal suspend fun <T> retryOnceOnTimeout(block: suspend () -> T): T =
    try {
        block()
    } catch (_: InterruptedIOException) {
        delay(TIMEOUT_RETRY_DELAY_MS)
        block()
    }
