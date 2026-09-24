package com.mvlog.agenttools.file

import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import okio.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

abstract class ChatFileBackendContract {

    internal abstract fun backend(): ChatFileBackend

    /** A root no other test touches. */
    internal abstract fun freshRoot(): Path

    internal abstract suspend fun cleanUp(root: Path)

    @Test
    fun aMissingDirectoryListsAsEmpty() = contract { backend, root ->
        assertEquals(emptyList(), backend.listFiles(root / "never-written"))
    }

    @Test
    fun aNestedWriteCreatesItsParents() = contract { backend, root ->
        backend.writeText(root / "a" / "b" / "c.txt", "deep")

        assertEquals("deep", backend.readText(root / "a" / "b" / "c.txt"))
    }

    @Test
    fun listingIsRecursiveRelativeAndSkipsDirectories() = contract { backend, root ->
        backend.writeText(root / "top.txt", "1")
        backend.writeText(root / "dir" / "inner.txt", "22")

        assertEquals(
            listOf(ChatFileEntry("dir/inner.txt", 2L), ChatFileEntry("top.txt", 1L)),
            backend.listFiles(root).sortedBy { it.path },
        )
    }

    @Test
    fun aMissingFileHasNoSizeAndNoText() = contract { backend, root ->
        assertNull(backend.size(root / "nope.txt"))
        assertNull(backend.readText(root / "nope.txt"))
    }

    @Test
    fun aDirectoryHasNoSizeAndNoText() = contract { backend, root ->
        backend.writeText(root / "dir" / "inner.txt", "x")

        assertNull(backend.size(root / "dir"), "a directory must not pass for a file")
        assertNull(backend.readText(root / "dir"), "a directory must not pass for a file")
    }

    @Test
    fun anOverwriteLeavesNoTailOfTheLongerOriginal() = contract { backend, root ->
        backend.writeText(root / "f.txt", "a much longer original")
        backend.writeText(root / "f.txt", "short")

        assertEquals("short", backend.readText(root / "f.txt"))
    }

    @Test
    fun nonAsciiRoundTripsAndSizeCountsUtf8Bytes() = contract { backend, root ->
        backend.writeText(root / "f.txt", "héllo — 日本")

        assertEquals("héllo — 日本", backend.readText(root / "f.txt"))
        assertEquals(
            "héllo — 日本".encodeToByteArray().size.toLong(),
            backend.size(root / "f.txt"),
            "the read limit is in bytes, so the size must be too",
        )
    }

    private fun contract(block: suspend (ChatFileBackend, Path) -> Unit): TestResult = runTest {
        val root = freshRoot()
        try {
            block(backend(), root)
        } finally {
            cleanUp(root)
        }
    }
}
