package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import okio.Path
import okio.Path.Companion.toPath

internal actual fun platformChatFileBackend(dispatchers: CoroutineDispatchers): ChatFileBackend =
    OpfsChatFileBackend()

internal actual fun platformFilesRoot(): Path = "/agent-files".toPath()
