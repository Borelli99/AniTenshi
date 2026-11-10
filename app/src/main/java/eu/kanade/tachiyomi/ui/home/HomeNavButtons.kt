package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.compositionLocalOf
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
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Room taken by the round buttons floating at the bottom of the main screens (0 when they are hidden).
 * Main screens lift their floating buttons by this much, so nothing ends up under the round buttons.
 */
val LocalBottomNavPadding = compositionLocalOf { 0.dp }

private val RingWidth = 3.dp
private val RingGap = 4.dp

/** Button sizes: a little larger on tablets, where the update badge is a little bigger too. */
private class NavSizes(
    val side: Dp,
    val home: Dp,
    val sideIcon: Dp,
    val homeIcon: Dp,
    /** Smallest width and height of the gold update badge. */
    val badge: Dp,
)

private val PhoneSizes = NavSizes(side = 60.dp, home = 80.dp, sideIcon = 26.dp, homeIcon = 44.dp, badge = 16.dp)
private val TabletSizes = NavSizes(side = 72.dp, home = 96.dp, sideIcon = 31.dp, homeIcon = 53.dp, badge = 20.dp)

/** How far the update badge sits in from the corner of its button's box, which puts it on the button's rim. */
private val BadgeInset = 2.dp

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
    vertical: Boolean = false,
) {
    val sizes = if (isTabletUi()) TabletSizes else PhoneSizes
    val animeSelected = current::class == AnimeLibraryTab::class
    val homeSelected = current::class == HomeTab::class
    val mangaSelected = current::class == MangaLibraryTab::class
    // The side buttons sit inside boxes as large as their ring: move them in until the circles overlap
    val sideShift = RingWidth + RingGap + RingWidth + RingGap + SideOverlap
    // In a row the side buttons slide sideways towards Home; in a column (tablet held sideways) they slide vertically
    val firstShift = if (vertical) Modifier.offset(y = sideShift) else Modifier.offset(x = sideShift)
    val lastShift = if (vertical) Modifier.offset(y = -sideShift) else Modifier.offset(x = -sideShift)
    // AniTenshi: the update badges sit on the outer rim of the side buttons, away from Home, which is drawn over
    // them and used to cover part of the Anime one. In a row: top outer corners. In a column: top of the first
    // button and bottom of the last, on the side of the content.
    val animeBadgeAlignment = if (vertical) Alignment.TopEnd else Alignment.TopStart
    val mangaBadgeAlignment = if (vertical) Alignment.BottomEnd else Alignment.TopEnd

    val buttons: @Composable () -> Unit = {
        NavCircle(
            selected = animeSelected,
            diameter = sizes.side,
            label = stringResource(AYMR.strings.label_anime_library),
            badge = animeBadge,
            badgeSize = sizes.badge,
            badgeAlignment = animeBadgeAlignment,
            onClick = { onClick(AnimeLibraryTab) },
            modifier = Modifier
                .zIndex(if (animeSelected) 3f else 1f)
                .then(firstShift),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_anime),
                contentDescription = null,
                tint = if (animeSelected) AniTenshiPalette.Ivory else AniTenshiPalette.Lavender,
                modifier = Modifier.size(sizes.sideIcon),
            )
        }
        NavCircle(
            selected = homeSelected,
            diameter = sizes.home,
            label = stringResource(MR.strings.label_home),
            badge = 0,
            badgeSize = sizes.badge,
            badgeAlignment = Alignment.TopEnd,
            onClick = { onClick(HomeTab) },
            modifier = Modifier.zIndex(if (homeSelected) 3f else 2f),
        ) {
            Image(
                painter = painterResource(
                    if (homeSelected) R.drawable.ic_nav_home_on else R.drawable.ic_nav_home_off,
                ),
                contentDescription = null,
                modifier = Modifier.size(sizes.homeIcon),
            )
        }
        NavCircle(
            selected = mangaSelected,
            diameter = sizes.side,
            label = stringResource(AYMR.strings.label_manga_library),
            badge = mangaBadge,
            badgeSize = sizes.badge,
            badgeAlignment = mangaBadgeAlignment,
            onClick = { onClick(MangaLibraryTab) },
            modifier = Modifier
                .zIndex(if (mangaSelected) 3f else 1f)
                .then(lastShift),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_manga),
                contentDescription = null,
                tint = if (mangaSelected) AniTenshiPalette.Ivory else AniTenshiPalette.Lavender,
                modifier = Modifier.size(sizes.sideIcon),
            )
        }
    }

    if (vertical) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { buttons() }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) { buttons() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavCircle(
    selected: Boolean,
    diameter: Dp,
    label: String,
    badge: Int,
    badgeSize: Dp,
    badgeAlignment: Alignment,
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
            // In from the corner it is aligned to, so it sits on the rim of the circle
            val insetX = if (badgeAlignment == Alignment.TopStart) BadgeInset else -BadgeInset
            val insetY = if (badgeAlignment == Alignment.BottomEnd) -BadgeInset else BadgeInset
            val large = badgeSize > PhoneSizes.badge
            Badge(
                containerColor = AniTenshiPalette.Gold,
                contentColor = AniTenshiPalette.Navy,
                modifier = Modifier
                    .align(badgeAlignment)
                    .offset(x = insetX, y = insetY)
                    .defaultMinSize(minWidth = badgeSize, minHeight = badgeSize),
            ) {
                if (large) {
                    Text(text = badge.toString(), style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(text = badge.toString())
                }
            }
        }
    }
}
