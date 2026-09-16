package com.mvlog.chat.presentation.ui.item.shimmer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mvlog.ui.ThoonPreview
import com.mvlog.ui.shimmer.TextShimmer

@Composable
fun ChatAiThoughtShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatItemShimmerBlock(modifier = Modifier.size(16.dp))
        TextShimmer(textLength = 10, fontSize = 13.sp)
    }
}

@Preview
@Composable
private fun ChatAiThoughtShimmerPreview() {
    ThoonPreview {
        ChatAiThoughtShimmer()
    }
}
