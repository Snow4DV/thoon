package com.mvlog.agent.impl.data.room.dao

import androidx.room.Embedded
import com.mvlog.agent.impl.data.room.entity.ChatEntity

/** matchedText is the whole part; the snippet is cut in Kotlin because it needs the query. */
data class ChatSearchRow(
    @Embedded val chat: ChatEntity,
    val messageSequence: Long,
    val matchedText: String,
    /** Unread; it is the min() that pins matchedText and messageSequence to one row. */
    val matchedPartSequence: Long,
)
