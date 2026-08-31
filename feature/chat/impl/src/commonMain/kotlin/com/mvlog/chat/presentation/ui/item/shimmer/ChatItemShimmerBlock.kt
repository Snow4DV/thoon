package com.mvlog.chat.presentation.ui.item.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.composables.ui.theme.colors
import com.composables.ui.theme.controlColor
import com.composeunstyled.theme.Theme
import com.valentinilk.shimmer.shimmer

/**
 * A grey shimmering block, sized entirely by [modifier], used to build up chat item skeletons
 * from icon-, bubble-, and dot-shaped pieces.
 */
@Composable
internal fun ChatItemShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier = modifier
            .shimmer()
            .clip(shape)
            .background(Theme[colors][controlColor]),
    )
}
