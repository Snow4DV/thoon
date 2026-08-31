package com.mvlog.agenttools.tools

import com.mvlog.agent.tool.ChatToolContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reading arguments a model wrote, which is not the same as reading arguments a compiler checked.
 *
 * A missing or wrong-typed field is routine — models produce them — so it must come back as a
 * sentence the model can act on rather than a cast exception it never sees.
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
