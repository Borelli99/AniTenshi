package tachiyomi.domain.release.model

/**
 * Contains information about the latest release.
 */
data class Release(
    val version: String,
    val info: String,
    val releaseLink: String,
    val downloadLink: String,
    // AniTenshi: true when this is the rolling "beta" pre-release, offered to beta testers
    val isBeta: Boolean = false,
)
