package com.mvlog.chat.presentation.ui.item.shimmer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mvlog.ui.ThoonPreview
import com.mvlog.ui.shimmer.TextShimmer

/**
 * Mirrors [com.mvlog.chat.presentation.ui.item.ChatToolChainCall]'s icon + tool name/action +
 * status header row.
 */
@Composable
fun ChatToolChainCallShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatItemShimmerBlock(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextShimmer(textLength = 10, fontSize = 13.sp)
            TextShimmer(textLength = 24, fontSize = 12.sp)
        }
        ChatItemShimmerBlock(modifier = Modifier.size(16.dp), shape = CircleShape)
    }
}

@Preview
@Composable
private fun ChatToolChainCallShimmerPreview() {
    ThoonPreview {
        ChatToolChainCallShimmer()
    }
}
