package eu.kanade.tachiyomi.ui.player.controls

import `is`.xyz.mpv.Utils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.AniTenshiPalette
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import eu.kanade.tachiyomi.ui.player.controls.components.playerCircle
import kotlin.math.abs
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MiddlePlayerControls(
    // previous
    hasPrevious: Boolean,
    onSkipPrevious: () -> Unit,

    // middle
    isLoading: Boolean,
    isLoadingEpisode: Boolean,
    controlsShown: Boolean,
    areControlsLocked: Boolean,
    showLoadingCircle: Boolean,
    paused: Boolean,
    gestureSeekAmount: Pair<Int, Int>?,
    onPlayPauseClick: () -> Unit,

    // next
    hasNext: Boolean,
    onSkipNext: () -> Unit,

    enter: EnterTransition,
    exit: ExitTransition,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.large),
    ) {
        AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = enter,
            exit = exit,
        ) {
            if (gestureSeekAmount == null) {
                ControlsButton(
                    Icons.Filled.SkipPrevious,
                    onClick = onSkipPrevious,
                    iconSize = 32.dp,
                    enabled = hasPrevious,
                    modifier = Modifier.playerCircle(),
                )
            }
        }

        val icon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
        val interaction = remember { MutableInteractionSource() }
        when {
            gestureSeekAmount != null -> {
                Text(
                    stringResource(
                        AYMR.strings.player_gesture_seek_indicator,
                        if (gestureSeekAmount.second >= 0) '+' else '-',
                        Utils.prettyTime(abs(gestureSeekAmount.second)),
                        Utils.prettyTime(gestureSeekAmount.first + gestureSeekAmount.second),
                    ),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(Color.Black, blurRadius = 5f),
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            (isLoading || isLoadingEpisode) && showLoadingCircle -> CircularProgressIndicator(Modifier.size(96.dp))

            else -> {
                AnimatedVisibility(
                    visible = controlsShown && !areControlsLocked,
                    enter = enter,
                    exit = exit,
                ) {
                    // AniTenshi: gold play/pause button
                    Image(
                        painter = rememberAnimatedVectorPainter(icon, !paused),
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(AniTenshiPalette.Gold)
                            .clickable(
                                interaction,
                                ripple(),
                                onClick = onPlayPauseClick,
                            )
                            .padding(18.dp),
                        colorFilter = ColorFilter.tint(AniTenshiPalette.Navy),
                        contentDescription = null,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsShown && !areControlsLocked,
            enter = enter,
            exit = exit,
        ) {
            if (gestureSeekAmount == null) {
                ControlsButton(
                    Icons.Filled.SkipNext,
                    onClick = onSkipNext,
                    iconSize = 32.dp,
                    enabled = hasNext,
                    modifier = Modifier.playerCircle(),
                )
            }
        }
    }
}
