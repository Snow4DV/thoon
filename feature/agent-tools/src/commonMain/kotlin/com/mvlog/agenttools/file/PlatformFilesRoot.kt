package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import okio.Path

internal expect fun platformChatFileBackend(dispatchers: CoroutineDispatchers): ChatFileBackend

internal expect fun platformFilesRoot(): Path
