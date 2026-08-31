package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/**
 * A chat's files on a real device filesystem.
 *
 * Every okio call here blocks, so all of them are moved off the caller's thread. That is also why
 * [ChatFileStore] suspends: the alternative is a run stalling the UI while a model reads a file.
 */
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
            // Refusing beats truncating: a silently clipped file would have the model reasoning
            // about content it cannot see and reporting conclusions drawn from half a document.
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

        // Ambiguity is refused rather than resolved: replacing the first of several matches is a
        // coin flip the caller did not ask for, and the model can disambiguate with more context.
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

        /** Enough for notes and short documents; not enough to bury a context window. */
        const val MAX_READ_BYTES = 256 * 1024L

    }
}

/**
 * The changed region plus a little of what surrounds it.
 *
 * Returned to the model so an edit is legible rather than a bare "ok" — including when a resumed
 * turn has applied it twice, which the surrounding text is what reveals.
 */
private fun String.excerptAround(start: Int, length: Int, context: Int = 200): String {
    val from = maxOf(0, start - context)
    val to = minOf(this.length, start + length + context)
    return buildString {
        if (from > 0) append("…")
        append(this@excerptAround.substring(from, to))
        if (to < this@excerptAround.length) append("…")
    }
}
