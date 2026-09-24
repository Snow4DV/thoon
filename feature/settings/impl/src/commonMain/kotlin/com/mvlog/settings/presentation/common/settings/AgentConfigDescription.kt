package com.mvlog.settings.presentation.common.settings

import com.mvlog.agent.api.model.AgentConfig

internal fun AgentConfig.describe(): String = when (this) {
    is AgentConfig.OpenAiCompatible -> "OpenAI-compatible · $modelId"
    is AgentConfig.Anthropic -> "Anthropic · $modelId"
    is AgentConfig.Ollama -> "Ollama · $modelId · $baseUrl"
    is AgentConfig.Local -> "On-device · $modelId"
}
