package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OkioChatFileStoreTest {

    @Test
    fun editReplacesTheMatchAndReturnsWhatTheFileNowSays() = runTest {
        val store = store()
        store.write(CHAT, "notes.md", "# Notes\nold line\n")

        val result = store.edit(CHAT, "notes.md", "old line", "new line")

        assertEquals("# Notes\nnew line\n", store.read(CHAT, "notes.md"))
        assertTrue(
            "new line" in result.excerpt,
            "the excerpt is what tells the model its edit landed, was: ${result.excerpt}",
        )
    }

    @Test
    fun aDoubleAppliedEditIsVisibleInTheResult() = runTest {
        // The shape a resumed turn can produce: the run wrote the file, died before recording it,
        // and the same edit is issued again. `new_text` contains `old_text`, so both the absent and
        // ambiguous checks pass and the text is genuinely applied twice.
        //
        // This is not prevented. The point of the echo is that the duplication reaches the model in
        // the text it reads back, instead of sitting silently in the file.
        val store = store()
        store.write(CHAT, "notes.md", "## Notes\n")

        store.edit(CHAT, "notes.md", "## Notes", "## Notes\n- buy milk")
        val second = store.edit(CHAT, "notes.md", "## Notes", "## Notes\n- buy milk")

        assertEquals(
            2,
            second.excerpt.split("- buy milk").size - 1,
            "the duplication must be legible in what the model reads back",
        )
    }

    @Test
    fun editFailsWhenTheTextIsNotThere() = runTest {
        val store = store()
        store.write(CHAT, "notes.md", "hello")

        val error = assertFailsWith<ChatFileException> {
            store.edit(CHAT, "notes.md", "goodbye", "hi")
        }
        assertTrue("not found" in error.message.orEmpty(), "was: ${error.message}")
    }

    @Test
    fun editRefusesAnAmbiguousMatch() = runTest {
        // Replacing the first of several is a coin flip the caller did not ask for, and silently
        // editing the wrong one is worse than asking for more context.
        val store = store()
        store.write(CHAT, "notes.md", "todo\ntodo\n")

        val error = assertFailsWith<ChatFileException> {
            store.edit(CHAT, "notes.md", "todo", "done")
        }
        assertTrue("more than once" in error.message.orEmpty(), "was: ${error.message}")
    }

    @Test
    fun oneChatCannotSeeAnothersFiles() = runTest {
        val store = store()
        store.write(CHAT, "secret.md", "mine")

        assertTrue(store.list("chat-2").isEmpty(), "files must be private to one conversation")
        assertFailsWith<ChatFileException> { store.read("chat-2", "../chat-1/secret.md") }
    }

    @Test
    fun listReportsRelativePathsAndSkipsDirectories() = runTest {
        val store = store()
        store.write(CHAT, "a/b.txt", "12345")

        assertEquals(listOf(ChatFileEntry("a/b.txt", 5L)), store.list(CHAT))
    }

    @Test
    fun readingAMissingFileSaysSoRatherThanReturningNothing() = runTest {
        val error = assertFailsWith<ChatFileException> { store().read(CHAT, "nope.md") }
        assertTrue("does not exist" in error.message.orEmpty(), "was: ${error.message}")
    }

    private fun store(): ChatFileStore = OkioChatFileStore(
        fileSystem = FakeFileSystem(),
        root = "/files".toPath(),
        dispatchers = TestDispatchers,
    )

    private object TestDispatchers : CoroutineDispatchers {
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val immediate: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private companion object {
        const val CHAT = "chat-1"
    }
}
