/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player.controls

import `is`.xyz.mpv.MPV
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import eu.kanade.tachiyomi.ui.player.controls.components.FilledControlsButton
import eu.kanade.tachiyomi.ui.player.controls.components.playerPill
import eu.kanade.tachiyomi.ui.player.execute
import eu.kanade.tachiyomi.ui.player.executeLongPress
import tachiyomi.domain.custombuttons.model.CustomButton
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BottomRightPlayerControls(
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
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PlayerSkipOrCustomButton(
            mpv = mpv,
            customButton = customButton,
            customButtonTitle = customButtonTitle,
            skipIntroButton = skipIntroButton,
            onPressSkipIntroButton = onPressSkipIntroButton,
        )
        PlayerPipAspectPill(
            isPipAvailable = isPipAvailable,
            onAspectClick = onAspectClick,
            onPipClick = onPipClick,
        )
    }
}

/**
 * AniTenshi: the button that skips an opening or ending, or else the custom button ("+85 s"), on its own, so the
 * narrow-screen layout ([CompactBottomPlayerControls]) can place it too. Nothing when there is neither.
 */
@Composable
fun PlayerSkipOrCustomButton(
    mpv: MPV,
    customButton: CustomButton?,
    customButtonTitle: String,
    skipIntroButton: String?,
    onPressSkipIntroButton: () -> Unit,
    modifier: Modifier = Modifier,
    endPadding: Dp = MaterialTheme.padding.small,
) {
    if (skipIntroButton != null) {
        FilledControlsButton(
            text = skipIntroButton,
            onClick = onPressSkipIntroButton,
            onLongClick = {},
            modifier = modifier,
            endPadding = endPadding,
        )
    } else if (customButton != null) {
        FilledControlsButton(
            text = customButtonTitle,
            onClick = { customButton.execute(mpv) },
            onLongClick = { customButton.executeLongPress(mpv) },
            modifier = modifier,
            highlighted = false,
            endPadding = endPadding,
        )
    }
}

/** AniTenshi: the pill with picture-in-picture and aspect ratio, on its own (see [PlayerSkipOrCustomButton]). */
@Composable
fun PlayerPipAspectPill(
    isPipAvailable: Boolean,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSpacing: Dp = MaterialTheme.padding.medium,
) {
    Row(modifier = modifier.playerPill()) {
        if (isPipAvailable) {
            ControlsButton(
                Icons.Default.PictureInPictureAlt,
                onClick = onPipClick,
                horizontalSpacing = buttonSpacing,
            )
        }

        ControlsButton(
            Icons.Default.AspectRatio,
            onClick = onAspectClick,
            horizontalSpacing = buttonSpacing,
        )
    }
}
