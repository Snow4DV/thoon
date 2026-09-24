package com.mvlog.agenttools.file

import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class OkioChatFileBackendTest : ChatFileBackendContract() {

    private val fileSystem = FakeFileSystem()

    override fun backend(): ChatFileBackend = OkioChatFileBackend(fileSystem, UnconfinedDispatchers)

    override fun freshRoot(): Path = "/files".toPath()

    override suspend fun cleanUp(root: Path) = Unit
}
