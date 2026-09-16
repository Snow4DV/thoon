package com.mvlog.agent.impl.execution

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.koog.KoogAgentRunner
import com.mvlog.agent.impl.koog.KoogClientFactory
import com.mvlog.agent.impl.koog.KoogToolRegistryFactory
import com.mvlog.agent.impl.koog.PersistentChatHistoryProvider
import com.mvlog.agent.impl.koog.RoomPersistenceStorageProvider
import com.mvlog.agent.impl.util.AgentClock

internal interface AgentRunnerFactory {

    fun create(config: AgentConfig?): AgentRunner
}

internal class DefaultAgentRunnerFactory(
    private val chatRepository: ChatRepository,
    private val historyProvider: PersistentChatHistoryProvider,
    private val persistenceStorage: RoomPersistenceStorageProvider,
    private val clientFactory: KoogClientFactory,
    private val toolRegistryFactory: KoogToolRegistryFactory,
    private val clock: AgentClock,
) : AgentRunnerFactory {

    override fun create(config: AgentConfig?): AgentRunner = when (config) {
        null -> throw UnsupportedConfigurationException(
            "No agent configuration selected. Add one and set it as the default."
        )

        is AgentConfig.OpenAiCompatible,
        is AgentConfig.Anthropic,
        is AgentConfig.Ollama,
        -> KoogAgentRunner(
            target = clientFactory.create(config),
            chatRepository = chatRepository,
            historyProvider = historyProvider,
            persistenceStorage = persistenceStorage,
            toolRegistryFactory = toolRegistryFactory,
            clock = clock,
        )

        is AgentConfig.Local -> throw UnsupportedConfigurationException(
            "'${config.name}' uses a local engine, which is not supported yet"
        )
    }
}

/** Caught by the executor; its message is shown to the user as the failure reason. */
internal class UnsupportedConfigurationException(message: String) : Exception(message)
