package com.mvlog.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ThoonTypography {
    /**
     * A screen's own name, at the top of it.
     *
     * Larger than [h1] and deliberately not bold: at this size weight reads as shouting, and the
     * title is meant to sit quietly above the content rather than compete with it.
     */
    val display = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Normal)

    val h1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val h2 = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val h3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val h4 = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val h5 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    val h6 = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
}
