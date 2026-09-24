package com.mvlog.agenttools.web

import com.mvlog.agent.tool.ChatToolContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FetchUrlToolTest {

    @Test
    fun aNetworkErrorThatIsNotAnExceptionIsReportedAsAToolFailure() = runTest {
        val engine = MockEngine { throw Error("Fail to fetch") }

        val error = assertFailsWith<WebToolException> {
            FetchUrlTool(HttpClient(engine)).execute(
                context = ChatToolContext(chatId = "chat"),
                arguments = JsonObject(mapOf("url" to JsonPrimitive("https://www.google.com"))),
            )
        }

        val message = error.message.orEmpty()
        assertTrue("https://www.google.com" in message, "the model must know which URL failed, was: $message")
        assertTrue("Fail to fetch" in message, "the model must know why, was: $message")
    }
}
