package com.mvlog.agent.api.model

/**
 * Why a configuration was rejected.
 *
 * Typed rather than a message string so a settings screen can attach the failure to the field that
 * caused it and localise the text.
 */
sealed interface AgentConfigError {

    data object BlankName : AgentConfigError

    data object BlankModelId : AgentConfigError

    data object MissingApiKey : AgentConfigError

    data object BlankEngineId : AgentConfigError

    data class MalformedBaseUrl(val value: String) : AgentConfigError

    data class NotFound(val id: AgentConfigId) : AgentConfigError
}

sealed interface AgentConfigResult<out T> {

    data class Success<T>(val value: T) : AgentConfigResult<T>

    data class Failure(val error: AgentConfigError) : AgentConfigResult<Nothing>
}

fun <T> AgentConfigResult<T>.getOrNull(): T? = (this as? AgentConfigResult.Success)?.value

fun AgentConfigResult<*>.errorOrNull(): AgentConfigError? =
    (this as? AgentConfigResult.Failure)?.error
