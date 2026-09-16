package com.mvlog.agent.api.model

sealed interface AgentConfigError {

    data object BlankName : AgentConfigError

    data object BlankModelId : AgentConfigError

    data object MissingApiKey : AgentConfigError

    /** Only for protocols with no default endpoint (Ollama). */
    data object MissingBaseUrl : AgentConfigError

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
