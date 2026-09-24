package com.mvlog.agenttools.file

import okio.Path

internal class SandboxedChatFileStore(
    private val backend: ChatFileBackend,
    private val root: Path,
) : ChatFileStore {

    override suspend fun list(chatId: String): List<ChatFileEntry> =
        backend.listFiles(chatRoot(chatId)).sortedBy { it.path }

    override suspend fun read(chatId: String, path: String): String {
        val file = ChatFilePaths.resolve(chatRoot(chatId), path)
        val size = backend.size(file) ?: throw missingForRead(path)
        if (size > MAX_READ_BYTES) {
            // Refuse rather than truncate: a clipped file has the model reasoning about text it
            // cannot see.
            throw ChatFileException(
                "'$path' is $size bytes, over the $MAX_READ_BYTES byte limit for reading.",
            )
        }

        return backend.readText(file) ?: throw missingForRead(path)
    }

    override suspend fun write(chatId: String, path: String, content: String) {
        backend.writeText(ChatFilePaths.resolve(chatRoot(chatId), path), content)
    }

    override suspend fun edit(
        chatId: String,
        path: String,
        oldText: String,
        newText: String,
    ): EditResult {
        if (oldText.isEmpty()) {
            throw ChatFileException("old_text must not be empty. Use write_file to create a file.")
        }

        val file = ChatFilePaths.resolve(chatRoot(chatId), path)
        val original = backend.readText(file)
            ?: throw ChatFileException("'$path' does not exist, so there is nothing to edit.")

        val first = original.indexOf(oldText)
        if (first < 0) {
            throw ChatFileException(
                "old_text was not found in '$path'. Read the file and match its exact text.",
            )
        }

        // Refuse ambiguity: replacing the first of several is a guess the caller did not make.
        if (original.indexOf(oldText, first + 1) >= 0) {
            throw ChatFileException(
                "old_text appears more than once in '$path'. Include enough surrounding text to " +
                    "identify which one you mean.",
            )
        }

        val updated = original.replaceRange(first, first + oldText.length, newText)
        backend.writeText(file, updated)

        return EditResult(path = path, excerpt = updated.excerptAround(first, newText.length))
    }

    private fun chatRoot(chatId: String): Path = root / CHATS_DIRECTORY / chatId

    private fun missingForRead(path: String) =
        ChatFileException("'$path' does not exist. Use list_files to see what does.")

    private companion object {
        const val CHATS_DIRECTORY = "chats"

        const val MAX_READ_BYTES = 256 * 1024L
    }
}

private fun String.excerptAround(start: Int, length: Int, context: Int = 200): String {
    val from = maxOf(0, start - context)
    val to = minOf(this.length, start + length + context)
    return buildString {
        if (from > 0) append("…")
        append(this@excerptAround.substring(from, to))
        if (to < this@excerptAround.length) append("…")
    }
}
