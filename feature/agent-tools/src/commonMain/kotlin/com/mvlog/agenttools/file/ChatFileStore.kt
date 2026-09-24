package com.mvlog.agenttools.file

internal interface ChatFileStore {

    suspend fun list(chatId: String): List<ChatFileEntry>

    suspend fun read(chatId: String, path: String): String

    suspend fun write(chatId: String, path: String, content: String)

    suspend fun edit(chatId: String, path: String, oldText: String, newText: String): EditResult
}

internal data class ChatFileEntry(val path: String, val sizeBytes: Long)

/** The excerpt is the model's confirmation that the edit landed where it meant. */
internal data class EditResult(val path: String, val excerpt: String)

internal class ChatFileException(message: String) : Exception(message)
