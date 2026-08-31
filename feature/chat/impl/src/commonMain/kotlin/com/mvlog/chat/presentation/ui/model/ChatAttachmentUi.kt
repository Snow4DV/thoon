package com.mvlog.chat.presentation.ui.model

data class ChatAttachmentUi(
    val name: String,
    val type: Type
) {

    enum class Type {
        IMAGE,
        TEXT_FILE,
        BINARY,
    }
}
