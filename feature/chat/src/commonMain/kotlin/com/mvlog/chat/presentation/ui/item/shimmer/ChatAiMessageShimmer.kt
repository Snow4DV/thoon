package com.mvlog.chat.presentation.ui.item.shimmer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mvlog.ui.ThoonPreview
import com.mvlog.ui.shimmer.TextShimmer

/** Mirrors [com.mvlog.chat.presentation.ui.item.ChatAiMessage]'s full-width paragraph of text. */
@Composable
fun ChatAiMessageShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TextShimmer(fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
        TextShimmer(fontSize = 15.sp, modifier = Modifier.fillMaxWidth(0.9f))
        TextShimmer(fontSize = 15.sp, modifier = Modifier.fillMaxWidth(0.6f))
    }
}

@Preview
@Composable
private fun ChatAiMessageShimmerPreview() {
    ThoonPreview {
        ChatAiMessageShimmer()
    }
}
