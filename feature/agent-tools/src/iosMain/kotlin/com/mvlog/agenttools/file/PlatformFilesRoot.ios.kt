package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/** Documents, not Caches: Caches can be evicted under storage pressure. */
@OptIn(ExperimentalForeignApi::class)
internal actual fun platformFilesRoot(): Path {
    val url: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    ) ?: error("Unable to resolve the iOS documents directory for agent files")

    return requireNotNull(url.path) { "Documents directory URL has no path" }.toPath()
}

internal actual fun platformChatFileBackend(dispatchers: CoroutineDispatchers): ChatFileBackend =
    OkioChatFileBackend(FileSystem.SYSTEM, dispatchers)
