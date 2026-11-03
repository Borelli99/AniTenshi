package eu.kanade.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import eu.kanade.domain.items.isUnknownUploadDate
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.util.lang.toRelativeString
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Instant

@Composable
fun relativeDateText(
    dateEpochMillis: Long,
): String {
    return relativeDateText(
        localDate = Instant.fromEpochMilliseconds(dateEpochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .takeIf { dateEpochMillis != 0L },
    )
}

// For use in chapter/episode release time
@Composable
fun relativeDateTimeText(
    dateEpochMillis: Long,
): String {
    return relativeDateTimeText(
        localDateTime = Instant.fromEpochMilliseconds(dateEpochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .takeIf { dateEpochMillis > 0L },
    )
}

@Composable
fun relativeDateText(
    localDate: LocalDate?,
): String {
    val context = LocalContext.current
    val locale = LocalLocale.current

    val preferences = remember { context.appGraph.uiPreferences }
    val relativeTime = remember { preferences.relativeTime.get() }
    val dateFormat = remember { UiPreferences.dateFormat(preferences.dateFormat.get(), locale.platformLocale) }

    return localDate?.toRelativeString(
        context = context,
        relative = relativeTime,
        dateFormat = dateFormat,
    )
        ?: stringResource(MR.strings.not_applicable)
}

// For use in chapter/episode release time
@Composable
fun relativeDateTimeText(
    localDateTime: LocalDateTime?,
): String {
    val context = LocalContext.current

    val preferences = remember { context.appGraph.uiPreferences }
    val relativeTime = remember { preferences.relativeTime.get() }
    val dateFormat = remember { UiPreferences.dateFormat(preferences.dateFormat.get()) }

    return localDateTime?.toRelativeString(
        context = context,
        relative = relativeTime,
        dateFormat = dateFormat,
    )
        ?: stringResource(MR.strings.not_applicable)
}

/**
 * AniTenshi: the release date to show for an episode or chapter, or null when the source gave none.
 *
 * When a source sends no date, the app stores the moment it found the item instead (needed for sorting),
 * which would read as "Now" for every episode, so such a date is hidden.
 */
fun displayedUploadDate(dateUpload: Long, dateUploadOverride: Long, dateFetch: Long): Long? = when {
    dateUploadOverride > 0L -> dateUploadOverride
    isUnknownUploadDate(dateUpload, dateFetch) -> null
    else -> dateUpload
}
