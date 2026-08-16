package com.mvlog.agent.impl.execution

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.koog.KoogAgentRunner
import com.mvlog.agent.impl.koog.KoogClientFactory
import com.mvlog.agent.impl.koog.PersistentChatHistoryProvider
import com.mvlog.agent.impl.koog.RoomPersistenceStorageProvider
import com.mvlog.agent.impl.util.AgentClock

/**
 * Chooses how a run is served, based on the configuration resolved for its chat.
 *
 * An interface so tests can supply a runner without reaching a network, and so adding a protocol
 * touches only the implementation — the executor's job stays "run one thing to a terminal state".
 */
internal interface AgentRunnerFactory {

    /** Throws [UnsupportedConfigurationException] when [config] cannot be served. */
    fun create(config: AgentConfig?): AgentRunner
}

internal class DefaultAgentRunnerFactory(
    private val chatRepository: ChatRepository,
    private val historyProvider: PersistentChatHistoryProvider,
    private val persistenceStorage: RoomPersistenceStorageProvider,
    private val clientFactory: KoogClientFactory,
    private val clock: AgentClock,
) : AgentRunnerFactory {

    override fun create(config: AgentConfig?): AgentRunner = when (config) {
        // Answering anyway — by echoing, or with a canned reply — would present a setup problem as
        // a working agent. Failing puts the reason in front of the user.
        null -> throw UnsupportedConfigurationException(
            "No agent configuration selected. Add one and set it as the default."
        )

        is AgentConfig.OpenAiCompatible,
        is AgentConfig.Anthropic,
        -> KoogAgentRunner(
            target = clientFactory.create(config),
            chatRepository = chatRepository,
            historyProvider = historyProvider,
            persistenceStorage = persistenceStorage,
            clock = clock,
        )

        is AgentConfig.Local -> throw UnsupportedConfigurationException(
            "'${config.name}' uses a local engine, which is not supported yet"
        )
    }
}

/** Fails the run with a message the user can act on, rather than a stack trace. */
internal class UnsupportedConfigurationException(message: String) : Exception(message)
