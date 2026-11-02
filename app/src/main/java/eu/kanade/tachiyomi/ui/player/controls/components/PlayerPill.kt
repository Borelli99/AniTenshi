package eu.kanade.tachiyomi.ui.player.controls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.AniTenshiPalette

/** AniTenshi: night blue, see-through, for the groups of player buttons. */
val PlayerPillColor = AniTenshiPalette.Navy.copy(alpha = 0.72f)
val PlayerPillBorder = AniTenshiPalette.NavyLine.copy(alpha = 0.9f)

/** Groups player buttons in a rounded night blue pill, as in the AniTenshi player. */
fun Modifier.playerPill(): Modifier = this
    .background(PlayerPillColor, CircleShape)
    .border(1.dp, PlayerPillBorder, CircleShape)
    .padding(horizontal = 4.dp)

/** A single round button in the same style. */
fun Modifier.playerCircle(): Modifier = this
    .background(PlayerPillColor, CircleShape)
    .border(1.dp, PlayerPillBorder, CircleShape)

/** Text on the pills: ivory, as in the AniTenshi theme. */
val PlayerTextColor: Color = AniTenshiPalette.Ivory
