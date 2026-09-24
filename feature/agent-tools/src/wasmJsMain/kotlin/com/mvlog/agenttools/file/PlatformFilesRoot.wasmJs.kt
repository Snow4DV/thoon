package com.mvlog.agenttools.file

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

private val browserFileSystem = FakeFileSystem()

internal actual fun platformFileSystem(): FileSystem = browserFileSystem

internal actual fun platformFilesRoot(): Path = "/".toPath()
