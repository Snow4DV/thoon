package com.mvlog.agenttools.file

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The sandbox.
 *
 * The model chooses these strings, and a web page it fetched may have suggested them, so an escape
 * attempt is an expected input rather than a hypothetical. This is the only thing standing between
 * one conversation's folder and the rest of the device.
 */
class ChatFilePathsTest {

    private val root = "/data/files/chats/chat-1".toPath()

    @Test
    fun aPlainRelativePathResolvesInsideTheChatFolder() {
        assertEquals(
            "/data/files/chats/chat-1/notes.md".toPath(),
            ChatFilePaths.resolve(root, "notes.md"),
        )
    }

    @Test
    fun nestedPathsAreAllowed() {
        assertEquals(
            "/data/files/chats/chat-1/a/b/c.txt".toPath(),
            ChatFilePaths.resolve(root, "a/b/c.txt"),
        )
    }

    @Test
    fun parentTraversalIsRejected() {
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, "../chat-2/notes.md") }
    }

    @Test
    fun traversalHiddenBehindRealSegmentsIsRejected() {
        // The one a prefix check would miss: this only reveals itself once `..` is resolved away,
        // which is why resolution normalises before it compares.
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, "a/../../chat-2/notes.md") }
    }

    @Test
    fun absolutePathsAreRejected() {
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, "/etc/passwd") }
    }

    @Test
    fun aSiblingFolderSharingOurPrefixIsRejected() {
        // `chat-1-notes` starts with `chat-1`, so a string-prefix containment check would let this
        // through. Containment is compared segment by segment for exactly this case.
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, "../chat-1-notes/x.md") }
    }

    @Test
    fun theChatFolderItselfIsNotAFile() {
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, ".") }
    }

    @Test
    fun anEmptyPathIsRejected() {
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, "   ") }
    }
}
