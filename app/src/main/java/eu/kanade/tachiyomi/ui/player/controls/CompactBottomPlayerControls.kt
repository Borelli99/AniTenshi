package eu.kanade.tachiyomi.ui.player.controls

import `is`.xyz.mpv.MPV
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.vivvvek.seeker.Segment
import eu.kanade.tachiyomi.ui.player.Sheets
import eu.kanade.tachiyomi.ui.player.controls.components.CurrentChapter
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import tachiyomi.domain.custombuttons.model.CustomButton
import tachiyomi.presentation.core.components.material.padding
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Below this window width (in dp) the player buttons don't fit side by side: a phone held upright. */
const val COMPACT_PLAYER_CONTROLS_MAX_WIDTH_DP = 520

/** Below this width the buttons inside the pills also stand closer together: small phones, large display size. */
private const val TIGHT_PLAYER_BUTTONS_MAX_WIDTH_DP = 360
private val TightButtonSpacing = 10.dp

/**
 * AniTenshi: the bottom player buttons on a narrow screen (a phone held upright).
 *
 * Side by side, as on a wide screen, they didn't fit: the speed button was squeezed until "1,00x" wrapped letter
 * by letter. Here the two pills keep one line, left and right as on a wide screen, and what comes and goes sits on
 * a line above them: the video chapter on the left, the skip (or custom, "+85 s") button on the right.
 */
@Composable
fun CompactBottomPlayerControls(
    playbackSpeed: Float,
    currentChapter: Segment?,
    onLockControls: () -> Unit,
    onCycleRotation: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onOpenSheet: (Sheets) -> Unit,
    mpv: MPV,
    customButton: CustomButton?,
    customButtonTitle: String,
    skipIntroButton: String?,
    onPressSkipIntroButton: () -> Unit,
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerPreferences = remember { Injekt.get<PlayerPreferences>() }
    val buttonSpacing = if (LocalConfiguration.current.screenWidthDp < TIGHT_PLAYER_BUTTONS_MAX_WIDTH_DP) {
        TightButtonSpacing
    } else {
        MaterialTheme.padding.medium
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            // The chapter takes the room the button on the right leaves, and shortens its name to fit
            Box(modifier = Modifier.weight(1f)) {
                // Named in full: inside the Box, the plain name would mean the Row's own variant, which can't be
                // called from here
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentChapter != null && playerPreferences.showCurrentChapter().get(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    currentChapter?.let { chapter ->
                        CurrentChapter(
                            chapter = chapter,
                            onClick = { onOpenSheet(Sheets.Chapters) },
                        )
                    }
                }
            }
            PlayerSkipOrCustomButton(
                mpv = mpv,
                customButton = customButton,
                customButtonTitle = customButtonTitle,
                skipIntroButton = skipIntroButton,
                onPressSkipIntroButton = onPressSkipIntroButton,
                endPadding = 0.dp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PlayerLockRotationSpeedPill(
                playbackSpeed = playbackSpeed,
                onLockControls = onLockControls,
                onCycleRotation = onCycleRotation,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
                onOpenSheet = onOpenSheet,
                buttonSpacing = buttonSpacing,
            )
            PlayerPipAspectPill(
                isPipAvailable = isPipAvailable,
                onAspectClick = onAspectClick,
                onPipClick = onPipClick,
                buttonSpacing = buttonSpacing,
            )
        }
    }
}
