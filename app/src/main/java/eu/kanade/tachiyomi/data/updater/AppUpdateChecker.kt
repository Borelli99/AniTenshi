package eu.kanade.tachiyomi.data.updater

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.util.system.isFossBuildType
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.release.interactor.GetApplicationRelease

@Inject
class AppUpdateChecker(
    private val getApplicationRelease: GetApplicationRelease,
) {

    suspend fun checkForUpdate(forceCheck: Boolean = false): GetApplicationRelease.Result {
        // Disable app update checks for older Android versions that we're going to drop support for
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        //    return GetApplicationRelease.Result.OsTooOld
        // }

        return withIOContext {
            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isFossBuildType,
                    // AniTenshi releases are tagged "r<commit count>", so they are compared by commit count
                    // like nightly builds.
                    true,
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    forceCheck,
                ),
            )

            result
        }
    }
}

const val GITHUB_REPO: String = Constants.GITHUB_REPO

val RELEASE_TAG: String = "r${BuildConfig.COMMIT_COUNT}"

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
