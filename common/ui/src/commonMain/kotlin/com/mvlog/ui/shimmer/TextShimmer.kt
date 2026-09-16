package com.mvlog.ui.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.theme.colors
import com.composables.ui.theme.controlColor
import com.composeunstyled.theme.Theme
import com.mvlog.ui.ThoonPreview
import com.valentinilk.shimmer.shimmer

private const val CHAR_WIDTH_RATIO = 0.58f
private const val LINE_HEIGHT_RATIO = 1.2f
private val SHIMMER_CORNER_RADIUS = 4.dp

@Composable
fun TextShimmer(
    textLength: Int,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val width = (fontSize.value * CHAR_WIDTH_RATIO * textLength).dp

    TextShimmer(
        fontSize = fontSize,
        modifier = modifier.width(width),
    )
}

@Composable
fun TextShimmer(
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val height = (fontSize.value * LINE_HEIGHT_RATIO).dp

    Box(
        modifier = modifier
            .shimmer()
            .height(height)
            .clip(RoundedCornerShape(SHIMMER_CORNER_RADIUS))
            .background(Theme[colors][controlColor]),
    )
}

@Preview
@Composable
fun TextShimmerPreview() {
    ThoonPreview {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextShimmer(textLength = 12, fontSize = 13.sp)
            TextShimmer(textLength = 28, fontSize = 14.sp)
            TextShimmer(textLength = 40, fontSize = 16.sp)
            TextShimmer(fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}
