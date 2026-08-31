package com.mvlog.agenttools.file

/**
 * The files belonging to one conversation.
 *
 * Suspending, and free of okio in its signatures, on purpose. okio backs it on Android and iOS, but
 * its `FileSystem` API is entirely synchronous while the browser's OPFS is not — and OPFS's one
 * synchronous door exists only inside a Web Worker. So an `OpfsFileSystem : FileSystem()` cannot be
 * written, and a web port needs a second implementation of *this* instead. Suspending also happens
 * to be right today: file I/O has no business on the main thread.
 */
internal interface ChatFileStore {

    suspend fun list(chatId: String): List<ChatFileEntry>

    suspend fun read(chatId: String, path: String): String

    suspend fun write(chatId: String, path: String, content: String)

    suspend fun edit(chatId: String, path: String, oldText: String, newText: String): EditResult
}

internal data class ChatFileEntry(val path: String, val sizeBytes: Long)

/**
 * What an edit did, including the text around it.
 *
 * The excerpt is the point: the model needs to see its edit landed where it meant, the same way it
 * would read a diff. It also makes an accidental double-apply legible — a resumed turn can re-issue
 * an edit whose `newText` contains its `oldText`, and reading the result back is what lets the model
 * notice and repair it.
 */
internal data class EditResult(val path: String, val excerpt: String)

/** Raised so the message reaches the model as the tool's result, which is what it can act on. */
internal class ChatFileException(message: String) : Exception(message)
