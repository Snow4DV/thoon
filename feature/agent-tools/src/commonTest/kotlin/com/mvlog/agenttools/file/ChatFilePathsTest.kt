package com.mvlog.agenttools.file

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, "a/../../chat-2/notes.md") }
    }

    @Test
    fun absolutePathsAreRejected() {
        assertFailsWith<ChatFileException> { ChatFilePaths.resolve(root, "/etc/passwd") }
    }

    @Test
    fun aSiblingFolderSharingOurPrefixIsRejected() {
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
