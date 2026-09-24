@file:JsModule("thoon-opfs-files")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.mvlog.agenttools.file

import kotlin.js.Promise

internal external fun listFiles(path: String): Promise<JsString>

internal external fun fileSize(path: String): Promise<JsNumber?>

internal external fun readText(path: String): Promise<JsString?>

internal external fun writeText(path: String, content: String): Promise<JsAny?>

internal external fun deleteRecursively(path: String): Promise<JsAny?>
