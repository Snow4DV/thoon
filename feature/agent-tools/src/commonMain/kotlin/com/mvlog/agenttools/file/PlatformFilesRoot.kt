package com.mvlog.agenttools.file

import okio.Path

/**
 * The directory this app may write to, resolved by the platform.
 *
 * The one genuinely platform-specific part of the store — everything else, including the sandbox
 * check, is shared.
 */
internal expect fun platformFilesRoot(): Path
