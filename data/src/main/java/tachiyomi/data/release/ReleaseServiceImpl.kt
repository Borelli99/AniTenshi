package tachiyomi.data.release

import android.os.Build
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ReleaseServiceImpl(
    private val networkService: NetworkHelper,
    private val json: Json,
) : ReleaseService {

    override suspend fun latest(arguments: GetApplicationRelease.Arguments): Release? {
        val stable = fetch("https://api.github.com/repos/${arguments.repository}/releases/latest")
        // AniTenshi: beta testers also see the rolling "beta" pre-release, when it is newer
        val release = if (arguments.includeBeta) newerBeta(arguments.repository, stable) ?: stable else stable
        val downloadLink = getDownloadLink(release = release) ?: return null

        return Release(
            version = release.version,
            info = release.info.replace(gitHubUsernameMentionRegex) { mention ->
                "[${mention.value}](https://github.com/${mention.value.substring(1)})"
            },
            releaseLink = release.releaseLink,
            downloadLink = downloadLink,
        )
    }

    private suspend fun fetch(url: String): GithubRelease = with(json) {
        networkService.client
            .newCall(GET(url))
            .awaitSuccess()
            .parseAs<GithubRelease>()
    }

    /**
     * AniTenshi: the pre-release with the fixed tag "beta". Its title ends with the build
     * number, like "AniTenshi 1.0.22 beta (r10055)": that number stands in for the tag.
     * Used only if it is newer than [stable] and its APKs are already there.
     */
    private suspend fun newerBeta(repository: String, stable: GithubRelease): GithubRelease? {
        val beta = try {
            fetch("https://api.github.com/repos/$repository/releases/tags/$BETA_TAG")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return null
        }
        val betaNumber = beta.name?.let { betaNumberRegex.find(it)?.groupValues?.get(1)?.toLongOrNull() }
            ?: return null
        val stableNumber = stable.version.filter(Char::isDigit).toLongOrNull() ?: 0L
        if (betaNumber <= stableNumber || getDownloadLink(beta) == null) return null
        return beta.copy(version = "r$betaNumber")
    }

    private fun getDownloadLink(release: GithubRelease): String? {
        val map = release.assets.associate { asset ->
            BUILD_TYPES.find { "-$it" in asset.name } to asset.downloadLink
        }

        return map[Build.SUPPORTED_ABIS[0]] ?: map[null]
    }

    companion object {
        private val BUILD_TYPES = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

        private const val BETA_TAG = "beta"
        private val betaNumberRegex = Regex("""\(r(\d+)\)""")

        /**
         * Regular expression that matches a mention to a valid GitHub username, like it's
         * done in GitHub Flavored Markdown. It follows these constraints:
         *
         * - Alphanumeric with single hyphens (no consecutive hyphens)
         * - Cannot begin or end with a hyphen
         * - Max length of 39 characters
         *
         * Reference: https://stackoverflow.com/a/30281147
         */
        private val gitHubUsernameMentionRegex = """\B@([a-z0-9](?:-(?=[a-z0-9])|[a-z0-9]){0,38}(?<=[a-z0-9]))"""
            .toRegex(RegexOption.IGNORE_CASE)
    }
}
