package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

internal class OkioChatFileBackend(
    private val fileSystem: FileSystem,
    private val dispatchers: CoroutineDispatchers,
) : ChatFileBackend {

    override suspend fun listFiles(dir: Path): List<ChatFileEntry> = io {
        if (!fileSystem.exists(dir)) return@io emptyList()

        fileSystem.listRecursively(dir)
            .mapNotNull { path ->
                val metadata = fileSystem.metadataOrNull(path)
                if (metadata?.isRegularFile != true) return@mapNotNull null
                ChatFileEntry(path = path.relativeTo(dir).toString(), sizeBytes = metadata.size ?: 0L)
            }
            .toList()
    }

    override suspend fun size(file: Path): Long? = io {
        fileSystem.metadataOrNull(file)?.takeIf { it.isRegularFile }?.let { it.size ?: 0L }
    }

    override suspend fun readText(file: Path): String? = io {
        if (fileSystem.metadataOrNull(file)?.isRegularFile != true) return@io null
        fileSystem.read(file) { readUtf8() }
    }

    override suspend fun writeText(file: Path, content: String): Unit = io {
        file.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.write(file) { writeUtf8(content) }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(dispatchers.io) { block() }
}
