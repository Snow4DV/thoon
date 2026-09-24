package com.mvlog.agenttools.file

import okio.FileSystem
import okio.Path

internal expect fun platformFileSystem(): FileSystem

internal expect fun platformFilesRoot(): Path
