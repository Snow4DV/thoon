package com.mvlog.agent.impl.koog

import ai.koog.prompt.message.Message
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The only place in the module that knows what a Koog [Message] is on the storage path.
 *
 * Everything else moves conversation history around as an opaque string, so swapping agent
 * frameworks — or changing how history is encoded — stays contained here.
 */
internal class KoogHistoryCodec(
    private val json: Json,
) {

    private val serializer = ListSerializer(Message.serializer())

    fun encode(messages: List<Message>): String = json.encodeToString(serializer, messages)

    fun decode(payload: String): List<Message> = json.decodeFromString(serializer, payload)

    companion object {

        /**
         * Bumped whenever an encoded payload stops being readable by this codec. Stored alongside
         * the payload so an incompatible history is discarded rather than crashing a run.
         */
        const val FORMAT_VERSION = 1
    }
}
