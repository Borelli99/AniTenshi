package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.theme.AniTenshiPalette
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource

private val RingWidth = 3.dp
private val RingGap = 4.dp
private val SideDiameter = 60.dp
private val HomeDiameter = 80.dp

/** How far the side buttons slide under the Home button, as in the AniTenshi sketch. */
private val SideOverlap = 8.dp

/**
 * AniTenshi's main buttons, in place of a bottom bar: Anime, Home (bigger, with the logo) and Manga.
 * They are night blue circles; the one in use wears the gold ring of the halo.
 * Holding a button shows its name.
 */
@Composable
internal fun HomeNavButtons(
    current: cafe.adriel.voyager.navigator.tab.Tab,
    animeBadge: Int,
    mangaBadge: Int,
    onClick: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animeSelected = current::class == AnimeLibraryTab::class
    val homeSelected = current::class == HomeTab::class
    val mangaSelected = current::class == MangaLibraryTab::class
    // The side buttons sit inside boxes as large as their ring: move them in until the circles overlap
    val sideShift = RingWidth + RingGap + RingWidth + RingGap + SideOverlap

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavCircle(
            selected = animeSelected,
            diameter = SideDiameter,
            label = stringResource(AYMR.strings.label_anime_library),
            badge = animeBadge,
            onClick = { onClick(AnimeLibraryTab) },
            modifier = Modifier
                .zIndex(if (animeSelected) 3f else 1f)
                .offset(x = sideShift),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_anime),
                contentDescription = null,
                tint = if (animeSelected) AniTenshiPalette.Ivory else AniTenshiPalette.Lavender,
                modifier = Modifier.size(26.dp),
            )
        }
        NavCircle(
            selected = homeSelected,
            diameter = HomeDiameter,
            label = stringResource(MR.strings.label_home),
            badge = 0,
            onClick = { onClick(HomeTab) },
            modifier = Modifier.zIndex(if (homeSelected) 3f else 2f),
        ) {
            Image(
                painter = painterResource(
                    if (homeSelected) R.drawable.ic_nav_home_on else R.drawable.ic_nav_home_off,
                ),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
        }
        NavCircle(
            selected = mangaSelected,
            diameter = SideDiameter,
            label = stringResource(AYMR.strings.label_manga_library),
            badge = mangaBadge,
            onClick = { onClick(MangaLibraryTab) },
            modifier = Modifier
                .zIndex(if (mangaSelected) 3f else 1f)
                .offset(x = -sideShift),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_manga),
                contentDescription = null,
                tint = if (mangaSelected) AniTenshiPalette.Ivory else AniTenshiPalette.Lavender,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavCircle(
    selected: Boolean,
    diameter: Dp,
    label: String,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(diameter + (RingWidth + RingGap) * 2),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            // The halo: a gold ring, separated from the button by a gap in the background color
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .border(RingWidth, AniTenshiPalette.Gold, CircleShape),
            )
        }
        TooltipBox(
            positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState(),
            focusable = false,
        ) {
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = AniTenshiPalette.Navy,
                contentColor = AniTenshiPalette.Ivory,
                border = if (selected) null else BorderStroke(1.dp, AniTenshiPalette.NavyLine),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(diameter)
                    .semantics {
                        contentDescription = label
                        this.selected = selected
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
        }
        if (badge > 0) {
            Badge(
                containerColor = AniTenshiPalette.Gold,
                contentColor = AniTenshiPalette.Navy,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp),
            ) {
                Text(text = badge.toString())
            }
        }
    }
}
