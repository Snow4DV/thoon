package com.mvlog.agenttools.file

import okio.Path
import okio.Path.Companion.toPath

/** The whole per-chat sandbox; `../` is an expected input. */
internal object ChatFilePaths {

    fun resolve(chatRoot: Path, relativePath: String): Path {
        val trimmed = relativePath.trim()
        if (trimmed.isEmpty()) {
            throw ChatFileException("A path is required.")
        }

        val candidate = trimmed.toPath()
        if (candidate.isAbsolute) {
            throw ChatFileException(
                "'$relativePath' is an absolute path. Use a path relative to this chat's folder.",
            )
        }

        // Normalise before the containment check: `a/../../b` only escapes once resolved.
        val resolved = chatRoot.resolve(candidate).normalized()
        if (!resolved.isWithin(chatRoot)) {
            throw ChatFileException(
                "'$relativePath' points outside this chat's folder. Files are private to one chat.",
            )
        }

        if (resolved == chatRoot) {
            throw ChatFileException("'$relativePath' is the chat folder itself, not a file.")
        }

        return resolved
    }

    /** Segment-wise rather than by string prefix, so `chat-1-notes` is not "within" `chat-1`. */
    private fun Path.isWithin(parent: Path): Boolean {
        val theseSegments = segments
        val parentSegments = parent.normalized().segments
        if (theseSegments.size <= parentSegments.size) return false
        return theseSegments.subList(0, parentSegments.size) == parentSegments
    }
}
