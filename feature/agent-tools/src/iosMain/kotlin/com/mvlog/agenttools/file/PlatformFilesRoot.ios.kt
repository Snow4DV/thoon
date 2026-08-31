package com.mvlog.agenttools.file

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Documents rather than Caches, for the same reason the database uses it: the system may evict
 * Caches under storage pressure, and a note the user asked the agent to keep should not vanish
 * because the device ran low on space.
 */
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
