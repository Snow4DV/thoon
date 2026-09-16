package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

internal class OkioChatFileStore(
    private val fileSystem: FileSystem,
    private val root: Path,
    private val dispatchers: CoroutineDispatchers,
) : ChatFileStore {

    override suspend fun list(chatId: String): List<ChatFileEntry> = io {
        val chatRoot = chatRoot(chatId)
        if (!fileSystem.exists(chatRoot)) return@io emptyList()

        fileSystem.listRecursively(chatRoot)
            .filter { fileSystem.metadataOrNull(it)?.isRegularFile == true }
            .map { path ->
                ChatFileEntry(
                    path = path.relativeTo(chatRoot).toString(),
                    sizeBytes = fileSystem.metadataOrNull(path)?.size ?: 0L,
                )
            }
            .toList()
            .sortedBy { it.path }
    }

    override suspend fun read(chatId: String, path: String): String = io {
        val file = ChatFilePaths.resolve(chatRoot(chatId), path)
        if (!fileSystem.exists(file)) {
            throw ChatFileException("'$path' does not exist. Use list_files to see what does.")
        }

        val size = fileSystem.metadataOrNull(file)?.size ?: 0L
        if (size > MAX_READ_BYTES) {
            // Refuse rather than truncate: a clipped file has the model reasoning about text it
            // cannot see.
            throw ChatFileException(
                "'$path' is $size bytes, over the $MAX_READ_BYTES byte limit for reading.",
            )
        }

        fileSystem.read(file) { readUtf8() }
    }

    override suspend fun write(chatId: String, path: String, content: String): Unit = io {
        val file = ChatFilePaths.resolve(chatRoot(chatId), path)
        file.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.write(file) { writeUtf8(content) }
    }

    override suspend fun edit(
        chatId: String,
        path: String,
        oldText: String,
        newText: String,
    ): EditResult = io {
        if (oldText.isEmpty()) {
            throw ChatFileException("old_text must not be empty. Use write_file to create a file.")
        }

        val file = ChatFilePaths.resolve(chatRoot(chatId), path)
        if (!fileSystem.exists(file)) {
            throw ChatFileException("'$path' does not exist, so there is nothing to edit.")
        }

        val original = fileSystem.read(file) { readUtf8() }
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
        fileSystem.write(file) { writeUtf8(updated) }

        EditResult(path = path, excerpt = updated.excerptAround(first, newText.length))
    }

    private fun chatRoot(chatId: String): Path = root / CHATS_DIRECTORY / chatId

    private suspend fun <T> io(block: () -> T): T = withContext(dispatchers.io) { block() }

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
