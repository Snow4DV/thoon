package com.mvlog.agenttools.file

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okio.Path

@OptIn(ExperimentalWasmJsInterop::class)
internal class OpfsChatFileBackend : ChatFileBackend {

    override suspend fun listFiles(dir: Path): List<ChatFileEntry> {
        val json = opfs { listFiles(dir.opfsPath()).await<JsString>().toString() }
        return Json.parseToJsonElement(json).jsonArray.map { element ->
            val entry = element.jsonObject
            ChatFileEntry(
                path = entry.getValue("path").jsonPrimitive.content,
                sizeBytes = entry.getValue("size").jsonPrimitive.long,
            )
        }
    }

    override suspend fun size(file: Path): Long? =
        opfs { fileSize(file.opfsPath()).await<JsNumber?>()?.toDouble()?.toLong() }

    override suspend fun readText(file: Path): String? =
        opfs { readText(file.opfsPath()).await<JsString?>()?.toString() }

    override suspend fun writeText(file: Path, content: String) {
        opfs { writeText(file.opfsPath(), content).await<JsAny?>() }
    }

    private fun Path.opfsPath(): String = segments.joinToString("/")

    private suspend fun <T> opfs(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val reason = e.message.orEmpty()
        throw ChatFileException(
            if ("QuotaExceededError" in reason) {
                "The browser's storage for this app is full; the file was not written."
            } else {
                "Browser file storage failed: $reason"
            },
        )
    }
}
