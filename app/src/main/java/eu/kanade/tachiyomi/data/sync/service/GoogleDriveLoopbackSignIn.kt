package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.tail.TLMR
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.UUID

/**
 * AniTenshi: Google Drive sign-in through the phone's own loopback address (http://127.0.0.1:port), the redirect
 * Google accepts for "Desktop app" OAuth clients. The custom scheme the app used before
 * (eu.kanade.google.oauth:/oauth2redirect) is no longer allowed by Google.
 *
 * The app listens on a free local port, opens the consent page in the browser, and waits for the one request the
 * browser makes back to it with the authorization code. Nothing leaves the phone except the call to Google.
 */
object GoogleDriveLoopbackSignIn {

    /** How long the app waits for the consent page to be completed. */
    private const val WAIT_FOR_CONSENT_MS = 5 * 60_000

    /** How long a single browser connection may take to send its request line. */
    private const val READ_REQUEST_MS = 10_000

    /** Browsers can open a few extra connections (pre-connect, favicon); this many are tolerated. */
    private const val MAX_CONNECTIONS = 10

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var server: ServerSocket? = null

    /** Opens the Google consent page and finishes the sign-in when the browser comes back. */
    fun start(context: Context) {
        val appContext = context.applicationContext
        server?.runCatching { close() }

        val socket = try {
            ServerSocket(0, MAX_CONNECTIONS, InetAddress.getByName("127.0.0.1"))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "AniTenshi: could not open the loopback port for Google sign-in" }
            appContext.toast(appContext.stringResource(TLMR.strings.google_drive_login_failed, e.message ?: ""))
            return
        }
        server = socket
        val redirectUri = "http://127.0.0.1:${socket.localPort}"
        val state = UUID.randomUUID().toString()
        val service = GoogleDriveService(appContext)

        val url = try {
            service.authorizationUrl(redirectUri, state)
        } catch (e: Exception) {
            socket.close()
            logcat(LogPriority.ERROR, e) { "AniTenshi: Google sign-in URL" }
            appContext.toast(appContext.stringResource(TLMR.strings.google_drive_login_failed, e.message ?: ""))
            return
        }

        scope.launch {
            val result = runCatching { waitForRedirect(socket, state) }
            socket.runCatching { close() }
            if (server === socket) server = null

            val answer = result.getOrElse { e ->
                logcat(LogPriority.ERROR, e) { "AniTenshi: Google sign-in redirect" }
                RedirectAnswer.Failed(e.message ?: e.javaClass.simpleName)
            }
            val message = when (answer) {
                is RedirectAnswer.Code -> runCatching { service.exchangeAuthorizationCode(answer.code, redirectUri) }
                    .fold(
                        onSuccess = {
                            // AniTenshi: first sync right away, instead of "Ultima sincronizzazione: Mai"
                            SyncDataJob.startNow(appContext)
                            appContext.stringResource(TLMR.strings.google_drive_login_success)
                        },
                        onFailure = { e ->
                            logcat(LogPriority.ERROR, e) { "AniTenshi: Google token exchange" }
                            appContext.stringResource(TLMR.strings.google_drive_login_failed, e.message ?: "")
                        },
                    )
                is RedirectAnswer.Failed ->
                    appContext.stringResource(TLMR.strings.google_drive_login_failed, answer.reason)
            }
            withUIContext { appContext.toast(message, duration = Toast.LENGTH_LONG) }
        }

        try {
            appContext.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            // No browser to open the consent page: stop listening
            socket.runCatching { close() }
            appContext.toast(appContext.stringResource(TLMR.strings.google_drive_login_failed, e.message ?: ""))
        }
    }

    private sealed interface RedirectAnswer {
        data class Code(val code: String) : RedirectAnswer
        data class Failed(val reason: String) : RedirectAnswer
    }

    /** Accepts browser connections until one carries Google's answer (a code or an error) for [state]. */
    private fun waitForRedirect(socket: ServerSocket, state: String): RedirectAnswer {
        socket.soTimeout = WAIT_FOR_CONSENT_MS
        repeat(MAX_CONNECTIONS) {
            val client = try {
                socket.accept()
            } catch (_: SocketTimeoutException) {
                return RedirectAnswer.Failed("timeout")
            }
            client.use { connection ->
                val query = readRequestQuery(connection)
                val params = query?.let { Uri.parse("http://127.0.0.1/?$it") }
                val code = params?.getQueryParameter("code")
                val error = params?.getQueryParameter("error")
                val stateOk = params?.getQueryParameter("state") == state
                when {
                    code != null && stateOk -> {
                        respond(connection, success = true)
                        return RedirectAnswer.Code(code)
                    }
                    error != null && stateOk -> {
                        respond(connection, success = false)
                        return RedirectAnswer.Failed(error)
                    }
                    else -> respondNotFound(connection)
                }
            }
        }
        return RedirectAnswer.Failed("no answer from the browser")
    }

    /** The query string of the request line ("GET /?code=…&state=… HTTP/1.1"), or null. */
    private fun readRequestQuery(connection: Socket): String? {
        connection.soTimeout = READ_REQUEST_MS
        val requestLine = runCatching {
            BufferedReader(InputStreamReader(connection.getInputStream(), Charsets.UTF_8)).readLine()
        }.getOrNull() ?: return null
        val target = requestLine.split(' ').getOrNull(1) ?: return null
        return target.substringAfter('?', missingDelimiterValue = "").takeIf { it.isNotEmpty() }
    }

    private fun respond(connection: Socket, success: Boolean) {
        val text = if (success) {
            "AniTenshi: accesso a Google Drive completato. Puoi tornare all'app."
        } else {
            "AniTenshi: accesso a Google Drive annullato. Puoi tornare all'app."
        }
        val body = """
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="background:#100B2C;color:#F6F2EA;font-family:sans-serif;display:flex;
            align-items:center;justify-content:center;height:90vh;text-align:center;padding:24px">
            <p style="font-size:20px">$text</p></body></html>
        """.trimIndent()
        writeResponse(connection, "200 OK", body)
    }

    private fun respondNotFound(connection: Socket) = writeResponse(connection, "404 Not Found", "")

    private fun writeResponse(connection: Socket, status: String, body: String) {
        runCatching {
            val bytes = body.toByteArray(Charsets.UTF_8)
            val head = "HTTP/1.1 $status\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
            connection.getOutputStream().apply {
                write(head.toByteArray(Charsets.US_ASCII))
                write(bytes)
                flush()
            }
        }
    }
}
