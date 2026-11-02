package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * AniTenshi theme ("Aureola"): the colors of the logo.
 * Dark: night blue background, ivory text, gold for the main actions and badges.
 * Light: ivory background, night blue text and buttons, gold for badges and highlights.
 */
internal object AniTenshiColorScheme : BaseColorScheme() {

    private val navy = Color(0xFF1B1446)
    private val ivory = Color(0xFFF6F2EA)
    private val gold = Color(0xFFF2C14E)

    override val darkScheme = darkColorScheme(
        primary = gold,
        onPrimary = navy,
        primaryContainer = gold,
        onPrimaryContainer = navy,
        inversePrimary = navy,
        secondary = gold, // Unread badge
        onSecondary = navy, // Unread badge text
        secondaryContainer = Color(0xFF352C72), // Navigation selector pill & progress indicator (remaining)
        onSecondaryContainer = ivory,
        tertiary = Color(0xFFB9B2D9), // Downloaded badge
        onTertiary = navy, // Downloaded badge text
        tertiaryContainer = Color(0xFF4A3F8C),
        onTertiaryContainer = ivory,
        background = Color(0xFF100B2C),
        onBackground = ivory,
        surface = Color(0xFF100B2C),
        onSurface = ivory,
        surfaceVariant = Color(0xFF241B57),
        onSurfaceVariant = Color(0xFFB9B2D9),
        surfaceTint = Color(0xFF8F88B8),
        inverseSurface = ivory,
        inverseOnSurface = navy,
        outline = Color(0xFF6B6399),
        outlineVariant = Color(0xFF2E2566),
        surfaceContainerLowest = Color(0xFF0B0722),
        surfaceContainerLow = Color(0xFF150F38),
        surfaceContainer = navy,
        surfaceContainerHigh = Color(0xFF241B57),
        surfaceContainerHighest = Color(0xFF2E2566),
    )

    override val lightScheme = lightColorScheme(
        primary = navy,
        onPrimary = ivory,
        primaryContainer = navy,
        onPrimaryContainer = ivory,
        inversePrimary = gold,
        secondary = gold, // Unread badge
        onSecondary = navy, // Unread badge text
        secondaryContainer = Color(0xFFE4DDF0), // Navigation selector pill & progress indicator (remaining)
        onSecondaryContainer = navy,
        tertiary = Color(0xFF5A5480), // Downloaded badge
        onTertiary = ivory, // Downloaded badge text
        tertiaryContainer = Color(0xFFE4DDF0),
        onTertiaryContainer = navy,
        background = ivory,
        onBackground = navy,
        surface = ivory,
        onSurface = navy,
        surfaceVariant = Color(0xFFECE5D7),
        onSurfaceVariant = Color(0xFF5A5480),
        surfaceTint = navy,
        inverseSurface = navy,
        inverseOnSurface = ivory,
        outline = Color(0xFF8C86A8),
        outlineVariant = Color(0xFFDED6C6),
        surfaceContainerLowest = Color(0xFFFFFDF8),
        surfaceContainerLow = Color(0xFFFBF8F1),
        surfaceContainer = Color(0xFFF2EDE2),
        surfaceContainerHigh = Color(0xFFECE5D7),
        surfaceContainerHighest = Color(0xFFE5DDCC),
    )
}
