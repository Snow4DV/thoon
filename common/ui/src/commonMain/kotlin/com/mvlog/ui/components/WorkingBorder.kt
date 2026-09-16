package com.mvlog.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.ui.theme.colors
import com.composables.ui.theme.fieldColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.ui.ThoonPreview

/**
 * One line segment, [segmentFraction] of the outline long, laps the edge of [shape] every
 * [periodMillis] while [active]. Inactive, the modifier is a no-op: no transition, no drawing.
 */
@Composable
fun Modifier.workingBorder(
    active: Boolean,
    shape: Shape,
    color: Color = Theme[colors][primaryColor],
    strokeWidth: Dp = 2.dp,
    segmentFraction: Float = 0.25f,
    periodMillis: Int = 1400,
): Modifier {
    if (!active) return this

    val transition = rememberInfiniteTransition(label = "working_border")
    val lap by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "working_border_lap",
    )

    return drawWithCache {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        // Inset by half the stroke so the whole line sits inside the bounds instead of being clipped.
        val outline = shape.createOutline(
            size = Size(size.width - stroke, size.height - stroke),
            layoutDirection = layoutDirection,
            density = this,
        )
        val path = Path().apply {
            addOutline(outline)
            translate(Offset(inset, inset))
        }
        val perimeter = PathMeasure().apply { setPath(path, forceClosed = true) }.length
        val segment = perimeter * segmentFraction

        onDrawWithContent {
            drawContent()
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(segment, perimeter - segment),
                        phase = (1f - lap) * perimeter,
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun WorkingBorderPreview() {
    val shape = RoundedCornerShape(16.dp)
    ThoonPreview {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .workingBorder(active = true, shape = shape)
                .background(Theme[colors][fieldColor], shape),
        )
    }
}
