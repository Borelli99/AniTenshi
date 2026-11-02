package eu.kanade.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.AniTenshiPalette
import eu.kanade.presentation.theme.AniTenshiTitleFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Size Android gives the launch icon when it has no background: the A here lines up with it. */
private val SplashIconSize = 288.dp

/** Like Android's overshoot interpolator: goes a little past the end, then settles. */
private val OvershootEasing = Easing { fraction ->
    val t = fraction - 1f
    t * t * (3f * t + 2f) + 1f
}

/**
 * AniTenshi: the launch animation, played over the app once per launch.
 *
 * Android's own launch screen only shows the A; here the halo drops onto it, glows,
 * and "AniTenshi" appears underneath, then everything fades into the app. A tap skips it.
 */
@Composable
fun LaunchAnimation(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val halo = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val screen = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(150)
        launch { halo.animateTo(1f, tween(durationMillis = 900, easing = OvershootEasing)) }
        delay(700)
        launch {
            glow.animateTo(1f, tween(durationMillis = 350, easing = LinearEasing))
            glow.animateTo(0.6f, tween(durationMillis = 500, easing = FastOutSlowInEasing))
        }
        delay(200)
        word.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        delay(450)
        screen.animateTo(0f, tween(durationMillis = 350, easing = FastOutSlowInEasing))
        finish()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screen.value }
            .background(AniTenshiPalette.Navy)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { finish() },
            ),
    ) {
        Canvas(modifier = Modifier.size(SplashIconSize)) {
            // Same drawing as the launcher icon: 108×108 box, scaled by 0.8 around its center
            val k = size.minDimension / 108f
            fun x(v: Float) = (54f + (v - 54f) * 0.8f) * k
            fun y(v: Float) = (54f + (v - 54f) * 0.8f - 0.7f) * k
            val unit = 0.8f * k

            val progress = halo.value
            val haloAlpha = (progress * 3f).coerceIn(0f, 1f)
            val drop = -24f * (1f - progress) * unit
            val haloTopLeft = Offset(x(39f), y(47.4f) + drop)
            val haloSize = Size(30f * unit, 9.2f * unit)
            val haloStroke = Stroke(width = 3.6f * unit, cap = StrokeCap.Round)

            // Soft gold light around the halo once it has landed
            if (glow.value > 0f) {
                val center = Offset(x(54f), y(52f))
                scale(scaleX = 1f, scaleY = 0.42f, pivot = center) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AniTenshiPalette.Gold.copy(alpha = 0.42f * glow.value),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = 30f * unit,
                        ),
                        radius = 30f * unit,
                        center = center,
                    )
                }
            }

            drawArc(
                color = AniTenshiPalette.Gold.copy(alpha = haloAlpha),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = haloTopLeft,
                size = haloSize,
                style = haloStroke,
            )
            val letter = Path().apply {
                moveTo(x(38f), y(77f))
                lineTo(x(54f), y(33f))
                lineTo(x(70f), y(77f))
            }
            drawPath(
                path = letter,
                color = AniTenshiPalette.Ivory,
                style = Stroke(width = 7.5f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawArc(
                color = AniTenshiPalette.Gold.copy(alpha = haloAlpha),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = haloTopLeft,
                size = haloSize,
                style = haloStroke,
            )
        }
        Text(
            text = "AniTenshi",
            color = AniTenshiPalette.Ivory,
            fontFamily = AniTenshiTitleFont,
            fontSize = 30.sp,
            letterSpacing = 0.02.em,
            modifier = Modifier
                .offset(y = 92.dp + 8.dp * (1f - word.value))
                .graphicsLayer { alpha = word.value },
        )
    }
}
