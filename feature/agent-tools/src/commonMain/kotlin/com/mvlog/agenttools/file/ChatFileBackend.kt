package com.mvlog.agenttools.file

import okio.Path

/** Storage only: paths arrive already resolved through `ChatFilePaths`. */
internal interface ChatFileBackend {

    /** Regular files under [dir], at any depth, with paths relative to it; empty if [dir] is absent. */
    suspend fun listFiles(dir: Path): List<ChatFileEntry>

    /** Null when [file] is absent or is not a regular file. */
    suspend fun size(file: Path): Long?

    /** Null when [file] is absent or is not a regular file. */
    suspend fun readText(file: Path): String?

    suspend fun writeText(file: Path, content: String)
}
