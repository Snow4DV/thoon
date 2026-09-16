package com.mvlog.agenttools.tools

import com.mvlog.agent.tool.ChatToolContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Model-written arguments are routinely missing or mistyped; the message is what the model reads
 * back.
 */
internal class MissingArgumentException(message: String) : Exception(message)

internal fun JsonObject.requireString(name: String): String {
    val value = this[name] ?: throw MissingArgumentException("'$name' is required.")
    val primitive = value as? JsonPrimitive
        ?: throw MissingArgumentException("'$name' must be a string.")
    if (!primitive.isString) {
        throw MissingArgumentException("'$name' must be a string.")
    }
    return primitive.content
}

internal fun JsonObject.optionalString(name: String): String? =
    this[name]?.let { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }

internal val ChatToolContext.chat: String get() = chatId
