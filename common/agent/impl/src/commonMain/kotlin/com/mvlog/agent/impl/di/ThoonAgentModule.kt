package com.mvlog.agent.impl.di

import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatsUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.StartAgentRuntimeUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase
import com.mvlog.agent.impl.data.mapper.AgentConfigMapper
import com.mvlog.agent.impl.data.memory.InMemoryAgentRunRepository
import com.mvlog.agent.impl.data.memory.InMemoryAgentStore
import com.mvlog.agent.impl.data.memory.InMemoryChatRepository
import com.mvlog.agent.impl.data.repository.RoomAgentConfigRepository
import com.mvlog.agent.impl.data.repository.RoomChatHistoryRepository
import com.mvlog.agent.impl.data.repository.RoomChatMetadataRepository
import com.mvlog.agent.impl.data.repository.RoomCheckpointRepository
import com.mvlog.agent.impl.domain.repository.AgentConfigRepository
import com.mvlog.agent.impl.domain.repository.AgentRunRepository
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import com.mvlog.agent.impl.domain.repository.ChatMetadataRepository
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.domain.repository.CheckpointRepository
import com.mvlog.agent.impl.domain.repository.ConversationRepository
import com.mvlog.agent.impl.domain.usecase.CancelAgentRunUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.CreateAgentConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.CreateChatUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.DeleteChatUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.RetryChatUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.DeleteAgentConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.GetAgentConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.ObserveAgentConfigsUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.ObserveChatConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.ObserveChatUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.ObserveChatsUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.SearchChatsUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.ObserveDefaultAgentConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.ResolveAgentConfigUseCase
import com.mvlog.agent.impl.domain.usecase.SendPromptUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.SetChatConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.SetDefaultAgentConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.StartAgentRuntimeUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.UpdateAgentConfigUseCaseImpl
import com.mvlog.agent.impl.domain.usecase.ValidateAgentConfigDraftUseCase
import com.mvlog.agent.impl.execution.AgentRunCoordinator
import com.mvlog.agent.impl.execution.AgentRunExecutor
import com.mvlog.agent.impl.execution.AgentRunnerFactory
import com.mvlog.agent.impl.execution.ChatRunMutexRegistry
import com.mvlog.agent.impl.execution.DefaultAgentRunnerFactory
import com.mvlog.agent.impl.koog.ChatTimelineProjector
import com.mvlog.agent.impl.koog.CheckpointCodec
import com.mvlog.agent.impl.koog.KoogConversationRepository
import com.mvlog.agent.impl.koog.KoogClientFactory
import com.mvlog.agent.impl.koog.KoogMessageRowCodec
import com.mvlog.agent.impl.koog.KoogToolRegistryFactory
import com.mvlog.agent.impl.koog.PersistentChatHistoryProvider
import com.mvlog.agent.impl.koog.RoomPersistenceStorageProvider
import com.mvlog.agent.impl.mapper.ChatStateApiMapper
import com.mvlog.database.dao.get

internal interface ThoonAgentModule {

    val chatRepository: ChatRepository
    val agentRunRepository: AgentRunRepository
    val chatHistoryRepository: ChatHistoryRepository
    val checkpointRepository: CheckpointRepository
    val chatMetadataRepository: ChatMetadataRepository
    val conversationRepository: ConversationRepository
    val agentConfigRepository: AgentConfigRepository

    val chatHistoryProvider: PersistentChatHistoryProvider
    val persistenceStorageProvider: RoomPersistenceStorageProvider

    val agentRunnerFactory: AgentRunnerFactory
    val runCoordinator: AgentRunCoordinator

    val startAgentRuntimeUseCase: StartAgentRuntimeUseCase

    val observeChatUseCase: ObserveChatUseCase
    val observeChatsUseCase: ObserveChatsUseCase
    val searchChatsUseCase: SearchChatsUseCase
    val createChatUseCase: CreateChatUseCase
    val retryChatUseCase: RetryChatUseCase

    val deleteChatUseCase: DeleteChatUseCase
    val sendPromptUseCase: SendPromptUseCase
    val cancelAgentRunUseCase: CancelAgentRunUseCase
    val observeChatConfigUseCase: ObserveChatConfigUseCase
    val setChatConfigUseCase: SetChatConfigUseCase

    val observeAgentConfigsUseCase: ObserveAgentConfigsUseCase
    val getAgentConfigUseCase: GetAgentConfigUseCase
    val observeDefaultAgentConfigUseCase: ObserveDefaultAgentConfigUseCase
    val createAgentConfigUseCase: CreateAgentConfigUseCase
    val updateAgentConfigUseCase: UpdateAgentConfigUseCase
    val deleteAgentConfigUseCase: DeleteAgentConfigUseCase
    val setDefaultAgentConfigUseCase: SetDefaultAgentConfigUseCase

    class Impl(
        dependencies: ThoonAgentComponentDependencies,
    ) : ThoonAgentModule, ThoonAgentComponentDependencies by dependencies {

        private val agentConfigMapper: AgentConfigMapper get() = AgentConfigMapper(json)

        private val chatStateApiMapper: ChatStateApiMapper get() = ChatStateApiMapper()

        private val historyCodec: KoogMessageRowCodec get() = KoogMessageRowCodec(json, idGenerator)

        private val checkpointCodec: CheckpointCodec get() = CheckpointCodec(json)

        /**
         * Backs the projections below. Stored, because those projections *are* its state — a fresh
         * instance per access would hand back an empty conversation.
         */
        private val inMemoryStore: InMemoryAgentStore = InMemoryAgentStore()

        /** The conversation as rendered. Rebuilt from durable state; nothing here outlives the process. */
        override val chatRepository: ChatRepository =
            InMemoryChatRepository(inMemoryStore, clock, idGenerator)

        /** The work queue. Recovery comes from checkpoints, not from these rows. */
        override val agentRunRepository: AgentRunRepository =
            InMemoryAgentRunRepository(inMemoryStore, clock)

        override val chatHistoryRepository: ChatHistoryRepository
            get() = RoomChatHistoryRepository(dao = daoFactory.get(), clock = clock)

        override val checkpointRepository: CheckpointRepository
            get() = RoomCheckpointRepository(dao = daoFactory.get())

        override val chatMetadataRepository: ChatMetadataRepository
            get() = RoomChatMetadataRepository(dao = daoFactory.get(), clock = clock)

        override val agentConfigRepository: AgentConfigRepository
            get() = RoomAgentConfigRepository(
                dao = daoFactory.get(),
                mapper = agentConfigMapper,
                clock = clock,
            )

        override val conversationRepository: ConversationRepository
            get() = KoogConversationRepository(
                historyRepository = chatHistoryRepository,
                checkpointRepository = checkpointRepository,
                metadataRepository = chatMetadataRepository,
                historyCodec = historyCodec,
                checkpointCodec = checkpointCodec,
                projector = ChatTimelineProjector(),
                clock = clock,
            )

        override val chatHistoryProvider: PersistentChatHistoryProvider
            get() = PersistentChatHistoryProvider(
                historyRepository = chatHistoryRepository,
                checkpointRepository = checkpointRepository,
                historyCodec = historyCodec,
                checkpointCodec = checkpointCodec,
            )

        override val persistenceStorageProvider: RoomPersistenceStorageProvider
            get() = RoomPersistenceStorageProvider(
                repository = checkpointRepository,
                codec = checkpointCodec,
            )

        private val koogClientFactory: KoogClientFactory get() = KoogClientFactory(httpClient)

        private val koogToolRegistryFactory: KoogToolRegistryFactory get() = KoogToolRegistryFactory()

        override val agentRunnerFactory: AgentRunnerFactory
            get() = DefaultAgentRunnerFactory(
                chatRepository = chatRepository,
                historyProvider = chatHistoryProvider,
                persistenceStorage = persistenceStorageProvider,
                clientFactory = koogClientFactory,
                toolRegistryFactory = koogToolRegistryFactory,
                clock = clock,
            )

        /**
         * Stored: it holds one mutex per chat, and that map is the guarantee that two runs on the
         * same chat never overlap. Separate registries would silently lose it.
         */
        private val chatRunMutexRegistry: ChatRunMutexRegistry = ChatRunMutexRegistry()

        private val agentRunExecutor: AgentRunExecutor
            get() = AgentRunExecutor(
                runnerFactory = agentRunnerFactory,
                resolveConfig = resolveAgentConfigUseCase,
                runRepository = agentRunRepository,
                chatRepository = chatRepository,
                mutexes = chatRunMutexRegistry,
            )

        /**
         * Stored: it owns the live per-chat workers and their cancellable jobs, so cancelling a run
         * has to reach the same instance that started it.
         */
        override val runCoordinator: AgentRunCoordinator by lazy {
            AgentRunCoordinator(
                runRepository = agentRunRepository,
                conversationRepository = conversationRepository,
                retryChat = retryChatUseCase,
                executor = agentRunExecutor,
                scope = processScope,
            )
        }

        private val resolveAgentConfigUseCase: ResolveAgentConfigUseCase
            get() = ResolveAgentConfigUseCase(agentConfigRepository, chatMetadataRepository)

        private val validateAgentConfigDraftUseCase: ValidateAgentConfigDraftUseCase
            get() = ValidateAgentConfigDraftUseCase()

        override val startAgentRuntimeUseCase: StartAgentRuntimeUseCase
            get() = StartAgentRuntimeUseCaseImpl(runCoordinator)

        override val observeChatUseCase: ObserveChatUseCase
            get() = ObserveChatUseCaseImpl(
                chatRepository = chatRepository,
                runRepository = agentRunRepository,
                conversationRepository = conversationRepository,
                mapper = chatStateApiMapper,
            )

        override val observeChatsUseCase: ObserveChatsUseCase
            get() = ObserveChatsUseCaseImpl(chatMetadataRepository)

        override val searchChatsUseCase: SearchChatsUseCase
            get() = SearchChatsUseCaseImpl(chatMetadataRepository)

        override val createChatUseCase: CreateChatUseCase
            get() = CreateChatUseCaseImpl(chatMetadataRepository, idGenerator)

        override val retryChatUseCase: RetryChatUseCase
            get() = RetryChatUseCaseImpl(
                conversationRepository = conversationRepository,
                runRepository = agentRunRepository,
                idGenerator = idGenerator,
            )

        override val deleteChatUseCase: DeleteChatUseCase
            get() = DeleteChatUseCaseImpl(chatMetadataRepository)

        override val sendPromptUseCase: SendPromptUseCase
            get() = SendPromptUseCaseImpl(
                chatRepository = chatRepository,
                runRepository = agentRunRepository,
                conversationRepository = conversationRepository,
                metadataRepository = chatMetadataRepository,
                idGenerator = idGenerator,
            )

        override val cancelAgentRunUseCase: CancelAgentRunUseCase
            get() = CancelAgentRunUseCaseImpl(
                runRepository = agentRunRepository,
                canceller = runCoordinator,
            )

        override val observeChatConfigUseCase: ObserveChatConfigUseCase
            get() = ObserveChatConfigUseCaseImpl(agentConfigRepository, chatMetadataRepository)

        override val setChatConfigUseCase: SetChatConfigUseCase
            get() = SetChatConfigUseCaseImpl(agentConfigRepository, chatMetadataRepository)

        override val observeAgentConfigsUseCase: ObserveAgentConfigsUseCase
            get() = ObserveAgentConfigsUseCaseImpl(agentConfigRepository)

        override val getAgentConfigUseCase: GetAgentConfigUseCase
            get() = GetAgentConfigUseCaseImpl(agentConfigRepository)

        override val observeDefaultAgentConfigUseCase: ObserveDefaultAgentConfigUseCase
            get() = ObserveDefaultAgentConfigUseCaseImpl(agentConfigRepository)

        override val createAgentConfigUseCase: CreateAgentConfigUseCase
            get() = CreateAgentConfigUseCaseImpl(
                repository = agentConfigRepository,
                validate = validateAgentConfigDraftUseCase,
                idGenerator = idGenerator,
            )

        override val updateAgentConfigUseCase: UpdateAgentConfigUseCase
            get() = UpdateAgentConfigUseCaseImpl(
                repository = agentConfigRepository,
                validate = validateAgentConfigDraftUseCase,
            )

        override val deleteAgentConfigUseCase: DeleteAgentConfigUseCase
            get() = DeleteAgentConfigUseCaseImpl(agentConfigRepository, chatMetadataRepository)

        override val setDefaultAgentConfigUseCase: SetDefaultAgentConfigUseCase
            get() = SetDefaultAgentConfigUseCaseImpl(agentConfigRepository)
    }
}
