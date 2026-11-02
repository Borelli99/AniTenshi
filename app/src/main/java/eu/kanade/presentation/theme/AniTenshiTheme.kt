package eu.kanade.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import eu.kanade.tachiyomi.R

/**
 * The AniTenshi colors, the same as the launcher icon: night blue, ivory and the gold of the halo.
 * Gold is kept for what is selected, the main action and the numbers worth noticing.
 */
object AniTenshiPalette {
    val Navy = Color(0xFF1B1446)
    val Night = Color(0xFF100B2C)
    val NavyLine = Color(0xFF2E2566)
    val Ivory = Color(0xFFF6F2EA)
    val Gold = Color(0xFFF2C14E)
    val Lavender = Color(0xFFB9B2D9)
    val MutedIcon = Color(0xFF8F88B8)
}

/** Zen Kaku Gothic New, bold: used for titles. Latin subset, SIL Open Font License (fonts/ in the repo). */
val AniTenshiTitleFont = FontFamily(Font(R.font.zen_kaku_gothic_new_bold, FontWeight.Bold))

private fun TextStyle.asTitle() = copy(fontFamily = AniTenshiTitleFont, fontWeight = FontWeight.Bold)

private val baseTypography = Typography()

/** Material typography with AniTenshi's title font on displays, headlines and large titles. */
val AniTenshiTypography = baseTypography.copy(
    displayLarge = baseTypography.displayLarge.asTitle(),
    displayMedium = baseTypography.displayMedium.asTitle(),
    displaySmall = baseTypography.displaySmall.asTitle(),
    headlineLarge = baseTypography.headlineLarge.asTitle(),
    headlineMedium = baseTypography.headlineMedium.asTitle(),
    headlineSmall = baseTypography.headlineSmall.asTitle(),
    titleLarge = baseTypography.titleLarge.asTitle(),
)
