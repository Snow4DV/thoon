package com.mvlog.agent.impl.fake

import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigUseCase
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
import com.mvlog.agent.impl.mapper.ChatStateApiMapper
import com.mvlog.agent.impl.fake.memory.InMemoryAgentConfigRepository
import com.mvlog.agent.impl.fake.memory.InMemoryChatMetadataRepository
import com.mvlog.agent.impl.fake.memory.InMemoryCheckpointRepository
import com.mvlog.agent.impl.data.memory.InMemoryAgentRunRepository
import com.mvlog.agent.impl.data.memory.InMemoryAgentStore
import com.mvlog.agent.impl.fake.memory.InMemoryChatHistoryRepository
import com.mvlog.agent.impl.data.memory.InMemoryChatRepository
import com.mvlog.agent.impl.di.ThoonAgentModule
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
import com.mvlog.agent.impl.domain.usecase.ObserveAgentConfigUseCaseImpl
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
import com.mvlog.agent.impl.koog.KoogToolRegistryFactory
import com.mvlog.agent.impl.koog.KoogMessageRowCodec
import com.mvlog.agent.impl.koog.PersistentChatHistoryProvider
import com.mvlog.agent.impl.koog.RoomPersistenceStorageProvider
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

/**
 * The agent graph on in-memory storage, with no database anywhere.
 *
 * Swapped in wholesale rather than patched into the production module, so a test can never
 * accidentally reach a DAO: this module has no dependencies at all.
 *
 * Storage is stored rather than `get()` — unlike the Room implementations, the in-memory ones hold
 * their own state, so a fresh instance per access would hand back an empty store.
 */
internal class TestAgentModule(
    private val agentScope: CoroutineScope,
    private val clock: AgentClock = AgentClock.System,
    private val idGenerator: IdGenerator = IdGenerator.Random,
    /**
     * True serves every run by echoing the prompt, for tests about the pipeline itself. False uses
     * the real factory, which needs a configuration before it will serve anything.
     */
    private val echoRuns: Boolean = false,
) : ThoonAgentModule {

    /** Mirrors the shared instance from `common:serialization`. */
    private val json = Json { ignoreUnknownKeys = true }

    private val store = InMemoryAgentStore()

    override val chatRepository: ChatRepository =
        InMemoryChatRepository(store, clock, idGenerator)

    override val agentRunRepository: AgentRunRepository =
        InMemoryAgentRunRepository(store, clock)

    override val chatHistoryRepository: ChatHistoryRepository = InMemoryChatHistoryRepository()

    override val checkpointRepository: CheckpointRepository = InMemoryCheckpointRepository()

    override val chatMetadataRepository: ChatMetadataRepository = InMemoryChatMetadataRepository()

    override val conversationRepository: ConversationRepository
        get() = KoogConversationRepository(
            historyRepository = chatHistoryRepository,
            checkpointRepository = checkpointRepository,
            metadataRepository = chatMetadataRepository,
            historyCodec = KoogMessageRowCodec(json, idGenerator),
            checkpointCodec = CheckpointCodec(json),
            projector = ChatTimelineProjector(),
            clock = clock,
        )

    override val chatHistoryProvider: PersistentChatHistoryProvider
        get() = PersistentChatHistoryProvider(
            historyRepository = chatHistoryRepository,
            checkpointRepository = checkpointRepository,
            historyCodec = KoogMessageRowCodec(json, idGenerator),
            checkpointCodec = CheckpointCodec(json),
        )

    override val persistenceStorageProvider: RoomPersistenceStorageProvider
        get() = RoomPersistenceStorageProvider(
            repository = checkpointRepository,
            codec = CheckpointCodec(json),
        )

    override val agentConfigRepository: AgentConfigRepository =
        InMemoryAgentConfigRepository()

    override val agentRunnerFactory: AgentRunnerFactory =
        if (echoRuns) {
            EchoAgentRunnerFactory(chatRepository)
        } else {
            DefaultAgentRunnerFactory(
                chatRepository = chatRepository,
                historyProvider = chatHistoryProvider,
                persistenceStorage = persistenceStorageProvider,
                clientFactory = KoogClientFactory(HttpClient()),
                toolRegistryFactory = KoogToolRegistryFactory(),
                clock = clock,
            )
        }

    private val chatRunMutexRegistry = ChatRunMutexRegistry()

    private val agentRunExecutor: AgentRunExecutor
        get() = AgentRunExecutor(
            runnerFactory = agentRunnerFactory,
            resolveConfig = resolveAgentConfigUseCase,
            runRepository = agentRunRepository,
            chatRepository = chatRepository,
            mutexes = chatRunMutexRegistry,
        )

    override val runCoordinator: AgentRunCoordinator = AgentRunCoordinator(
        runRepository = agentRunRepository,
        conversationRepository = conversationRepository,
        retryChat = retryChatUseCase,
        executor = agentRunExecutor,
        scope = agentScope,
    )

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
            mapper = ChatStateApiMapper(),
        )

    override val searchChatsUseCase: SearchChatsUseCase
        get() = SearchChatsUseCaseImpl(chatMetadataRepository)

    override val observeChatsUseCase: ObserveChatsUseCase
        get() = ObserveChatsUseCaseImpl(chatMetadataRepository)

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

    override val observeAgentConfigUseCase: ObserveAgentConfigUseCase
        get() = ObserveAgentConfigUseCaseImpl(agentConfigRepository)

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
