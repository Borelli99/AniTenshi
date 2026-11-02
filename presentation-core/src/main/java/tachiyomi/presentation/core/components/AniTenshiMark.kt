package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val HaloGold = Color(0xFFF2C14E)
private val HornRedDark = Color(0xFFE5383B)
private val HornRedLight = Color(0xFFD62F2F)

/**
 * The AniTenshi "A" used as a quiet illustration in empty screens.
 *
 * Normally the A wears its gold halo; when something went wrong ([isError]) the halo is
 * swapped for a pair of red devil horns.
 *
 * Geometry matches the launcher icon: a 56×56 box starting at (26, 27).
 */
@Composable
fun AniTenshiMark(
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    size: Dp = 88.dp,
    /** The A's color; by default a quiet tint for empty screens. Pass e.g. onBackground for the full logo. */
    letterColor: Color = Color.Unspecified,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val letterTint = letterColor.takeOrElse { lerp(colors.secondaryContainer, colors.onSurfaceVariant, 0.12f) }
    val hornColor = if (isDark) HornRedDark else HornRedLight

    Spacer(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val s = this.size.minDimension / 56f
                fun x(v: Float) = (v - 26f) * s
                fun y(v: Float) = (v - 27f) * s

                val letter = Path().apply {
                    moveTo(x(38f), y(77f))
                    lineTo(x(54f), y(33f))
                    lineTo(x(70f), y(77f))
                }
                val letterStroke = Stroke(
                    width = 7.5f * s,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )

                val haloTopLeft = Offset(x(39f), y(47.4f))
                val haloSize = Size(30f * s, 9.2f * s)
                val haloStroke = Stroke(width = 3f * s, cap = StrokeCap.Round)

                // Left horn; the right one is its mirror around x = 54.
                fun horn(mirror: Boolean) = Path().apply {
                    fun hx(v: Float) = x(if (mirror) 108f - v else v)
                    moveTo(hx(47.6f), y(47.5f))
                    cubicTo(hx(41.5f), y(46.8f), hx(38.6f), y(41.5f), hx(39.4f), y(35.5f))
                    cubicTo(hx(39.8f), y(32.6f), hx(40.9f), y(30.2f), hx(42.6f), y(28.2f))
                    cubicTo(hx(42.5f), y(33.2f), hx(45.2f), y(38.4f), hx(49.9f), y(40.8f))
                    close()
                }
                val hornLeft = horn(mirror = false)
                val hornRight = horn(mirror = true)

                onDrawBehind {
                    if (isError) {
                        drawPath(hornLeft, hornColor)
                        drawPath(hornRight, hornColor)
                        drawPath(letter, letterTint, style = letterStroke)
                    } else {
                        // Back half of the halo, then the A, then the front half on top.
                        drawArc(HaloGold, 180f, 180f, false, haloTopLeft, haloSize, style = haloStroke)
                        drawPath(letter, letterTint, style = letterStroke)
                        drawArc(HaloGold, 0f, 180f, false, haloTopLeft, haloSize, style = haloStroke)
                    }
                }
            },
    )
}
