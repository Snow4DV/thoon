package com.mvlog.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.CenteredToolbar
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text

@Composable
fun ThoonTopBar(
    title: String,
    onBackClicked: (() -> Unit)?,
    onOptionsClick: (() -> Unit)?,
    modifier: Modifier,
) {
    CenteredToolbar(
        modifier = modifier,
        title = {
            Text(title)
        },
        leading = {
            onBackClicked?.let {
                IconButton(
                    onClick = onBackClicked,
                    style = ButtonStyle.Ghost
                ) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Go back")
                }
            }
        },
        trailing = {
            onOptionsClick?.let {
                IconButton(
                    onClick = onOptionsClick,
                    style = ButtonStyle.Ghost,
                ) {
                    Icon(
                        imageVector = Lucide.EllipsisVertical,
                        contentDescription = "Open options",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
    )
}