package eu.kanade.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.AniTenshiPalette
import eu.kanade.presentation.theme.TachiyomiPreviewTheme

@Composable
fun PageIndicatorText(
    // SY -->
    currentPage: String,
    // SY <--
    totalPages: Int,
) {
    if (currentPage.isEmpty() || totalPages <= 0) return

    // AniTenshi: the page number in a small night blue pill
    Text(
        text = "$currentPage / $totalPages",
        color = AniTenshiPalette.Ivory,
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(bottom = 10.dp)
            .clip(CircleShape)
            .background(AniTenshiPalette.Navy.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** AniTenshi: thin gold line along the bottom with how far into the chapter you are. */
@Composable
fun ReaderProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(AniTenshiPalette.Navy.copy(alpha = 0.25f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(AniTenshiPalette.Gold),
        )
    }
}

@PreviewLightDark
@Composable
private fun PageIndicatorTextPreview() {
    TachiyomiPreviewTheme {
        Surface {
            PageIndicatorText(currentPage = "10", totalPages = 69)
        }
    }
}
