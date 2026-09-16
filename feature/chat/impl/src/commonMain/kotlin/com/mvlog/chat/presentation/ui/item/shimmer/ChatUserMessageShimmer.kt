package com.mvlog.chat.presentation.ui.item.shimmer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mvlog.ui.ThoonPreview

@Composable
fun ChatUserMessageShimmer(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopEnd,
    ) {
        ChatItemShimmerBlock(
            modifier = Modifier.width(180.dp).height(50.dp),
            shape = RoundedCornerShape(10.dp),
        )
    }
}

@Preview
@Composable
private fun ChatUserMessageShimmerPreview() {
    ThoonPreview {
        ChatUserMessageShimmer()
    }
}
