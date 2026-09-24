package com.mvlog.agenttools.file

import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalWasmJsInterop::class)
class OpfsChatFileBackendTest : ChatFileBackendContract() {

    override fun backend(): ChatFileBackend = OpfsChatFileBackend()

    override fun freshRoot(): Path = "/test-${Random.nextLong().toULong()}".toPath()

    @Test
    fun aBrowserStorageErrorReachesTheModelAsAFileErrorWithItsCause() = runTest {
        val root = freshRoot()
        try {
            val backend = backend()
            backend.writeText(root / "file.txt", "x")

            val error = assertFailsWith<ChatFileException> {
                backend.writeText(root / "file.txt" / "child.txt", "y")
            }
            assertTrue(
                "TypeMismatchError" in error.message.orEmpty(),
                "the DOMException's name is the only clue to what went wrong, was: ${error.message}",
            )
        } finally {
            cleanUp(root)
        }
    }

    override suspend fun cleanUp(root: Path) {
        deleteRecursively(root.segments.joinToString("/")).await<JsAny?>()
    }
}
