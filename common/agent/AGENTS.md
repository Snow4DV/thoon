# common/agent

The agent subsystem: `api` (use cases and models), `tool-api` (the Koog-free tool contract) and
`impl` (runtime, storage, Koog). How it works is in `docs/architecture/AGENT_RUNTIME.md` and
`docs/architecture/STORAGE.md`. This file holds the decisions and traps specific to these modules.
Each is stated once, here, and not in code. Koog is pinned at 1.1.1.

## Koog 1.1.1 gaps and JVM-only APIs

- No `SingleLLMPromptExecutor`; `MultiLLMPromptExecutor` with one client is the equivalent.
- No `List<StreamFrame>.toMessageResponse`; `StreamFrameAssembler` is named for it so adopting the
  library's is a deletion.
- `openAIClient()`, `anthropicClient()`, `ollamaClient()`, `HttpClientFactoryResolver.resolve()`,
  `ToolSet`, `@Tool` and `asTools()` are `jvmCommonMain`-only. Use the client constructors,
  `KtorKoogHttpClient.Factory(baseClient = …)` (which also shares the app's one connection pool),
  and hand-written descriptors.
- The non-factory `KoogHttpClient` overload of `OpenAILLMClient` takes no `apiKey`; auth arrives
  through `Factory.create(…, headers, …)`, so passing a pre-built client silently drops the key.
- `OllamaClient.executeStreaming` omits `tools` from the request. `ToolForwardingOllamaClient`
  re-injects them through `params.additionalProperties`, the one field the delegate flattens into the
  request root, and reuses Koog's own schema generator so the shape matches the non-streaming path.
  Delete the decorator when upstream fixes it.

## Client and capability configuration

- `LLMCapability.OpenAIEndpoint.Completions` must be declared or the OpenAI client targets the
  Responses API, which most OpenAI-compatible servers do not implement. Ollama gets no
  `OpenAIEndpoint` at all.
- Capability lists are written with an explicit `<LLMCapability>` element type: Native infers
  `List<Any>` and fails.
- Ollama `think = true` is set explicitly so the timeline shows reasoning for every model rather than
  whichever the per-model default allows. Per-protocol prompt parameters travel in `KoogTarget.params`.
- Koog's `OllamaClient` defaults to `http://localhost:11434`, which on a device or emulator is the
  device itself, so `AgentConfigDraft.Ollama` requires a base URL. Draft validation
  (`ValidateAgentConfigDraftUseCase`) checks only scheme and host on purpose, so private hosts pass.

## The graph

- Call `graphStrategy` before any `install`: a feature installed first pins the agent to
  `<String, String>` and the error surfaces somewhere else.
- One conversation is re-sent in full per round and there is no planner node: the model's reasoning
  and its tool calls arrive in the same assistant turn. Nodes yield `Message.Assistant`, not text,
  because a turn may be reasoning plus a tool call with no prose and the edges need the calls.
- Stream frames are collected inside `llm.writeSession`, not returned as a `Flow`.
  `nodeLLMSendToolResultsStreaming` returns a `Flow` nobody would collect, so frames would never reach
  the sink, and `respond` cannot be re-entered because it appends the prompt each time.
- `requestLLMStreaming` does not append the reply to the prompt (`requestLLM` does). The runner
  appends after the stream completes, so a run that dies mid-stream leaves no half-formed message.
- The tool budget is bounded by the edge predicate, not by the nudge message; a model can ignore a
  message. The nudge exists so the turn ends with an answer instead of mid-loop. The finish edge is
  unconditional so it is also the exit once the budget is spent.
- Streaming writes to the timeline are unguarded on purpose: if the timeline cannot be written the
  run should fail, not finish with half a transcript.

## Storage decisions

- Koog's session id is the chat id. `agent.run(prompt, chatId)` is the only place it is set;
  `ChatMemory` and `Persistence` both key storage by it.
- History is loaded at run start and committed whole at run end, so two overlapping runs on one chat
  would each commit a snapshot ignoring the other. One worker per chat is the first guard;
  `ChatRunMutexRegistry` is the second, and it is what tests and direct `KoogAgentRunner` callers
  rely on. Both registries are stored singletons; two would hand two runs two different mutexes.
- `load` drops a trailing user prompt (the run re-sends it as input) but keeps trailing tool results,
  which a resumed turn needs.
- Unreadable history is logged and dropped whole; an unreadable checkpoint row is skipped. Half
  restored is worse than empty, and throwing would brick the chat.
- Two repositories sit over `agent_chat`: `ChatMetadataRepository` (list, title, override; never the
  conversation) and `ChatHistoryRepository` (message and part rows). `agent_chat.lastMessageAt` is
  the last turn's commit time and `updatedAt` bumps on any write, so both lists order by
  `coalesce(lastMessageAt, createdAt)` and never by `updatedAt`. Summary columns are written in the
  commit transaction so the list never describes a turn the conversation lacks.
- `agent_checkpoint` has no foreign key to `agent_chat`; `deleteChat` removes checkpoints by hand.
- A chat is titled from its first prompt only: first non-blank line, 60 characters. A blank prompt
  stores no title, so the list's id-derived fallback survives.
- Search returns one row per matching turn (`GROUP BY m.id`), windows the snippet around the match in
  Kotlin because the head of a reply rarely contains it, bounds the flow with `SEARCH_LIMIT` and caps
  `MATCHES_PER_CHAT` so one chat cannot fill the list. `escapeLike` pairs with `ESCAPE '\'`;
  unescaped, a query of `50%` matches everything. An unknown part kind is logged and dropped; an
  unknown role drops the whole history.

## Configuration storage

- Configurations are keyed by protocol, not vendor: `OpenAiCompatible` serves OpenAI, OpenRouter,
  Groq, Together, LM Studio, vLLM and llama.cpp alike.
- `agent_config` is the shared columns plus `payloadJson` (`StoredAgentConfigPayload`, whose
  `@SerialName` equals the `kind` column). A new protocol is a payload variant and a mapper branch,
  not a migration. The API key lives in that JSON, in plaintext (see the root security constraints).
- The default is `agent_settings.defaultConfigId`, a singleton row. The first configuration created
  becomes the default; deleting the default leaves none rather than promoting another, because an
  unset default is visible and a reassigned one is not. `deleteAndDetach` nulls it in the delete
  transaction; `DeleteAgentConfigUseCase` detaches the chats.
- The per-chat override is a column on the chat. Resolution (override, else default) is only ever a
  use case: `ObserveChatConfigUseCase` for screens, `ResolveAgentConfigUseCase` for the executor, per
  run so in-flight runs keep the config they started with. A dangling override falls back to the
  default.
- `AgentConfig.Local` is reserved. A `LocalEngine` descriptor alone is not an engine;
  `KoogClientFactory` must also serve it or every run fails with an explicit message. The registry is
  a list rather than a free-text `engineId` so the editor can say it is empty.
- `GetAgentConfigUseCase` is one-shot by design (see `docs/circuit/PRESENTER.md`); do not add a flow
  variant.

## What the system prompt is for

Tool schemas say what a tool accepts, not when to use it, that files outlive the session, or that
fetched text is not an instruction. A local model given schemas and no brief ignores them or calls
them at random. That is what `AGENT_SYSTEM_PROMPT` is for, and why it is restored with the history.

## Tool approval

- The run ends through `nodeFinish` when a call needs a decision, never by throwing: only a normal
  finish commits the turn and writes the tombstone that lets the next run start at `resume`.
- The tool edges' second condition asks the resolver on every turn, so a stored rule applies
  mid-conversation without a restart; a rule added from settings also calls `retryChat` for the
  chats it may have unblocked.
- A `Declined` verdict can only appear on the resume path: decisions are recorded while no run is
  live, and the executor clears them on exit. The live tool edges therefore check only "executes".
- **No node before `respond`.** `Persistence` checkpoints after every node, and `ChatMemory.load`
  drops the trailing prompt because the run re-sends it as input, so a node between `nodeStart` and
  `respond` would be checkpointed with a history that lacks the prompt. The timeline and the next
  run's replay both read the latest live checkpoint, so a run dying in `respond` would lose the
  prompt from view and re-send only it. The resume decision is therefore taken on the edges out of
  `nodeStart`, computed once per run and cached in the strategy closure like `toolRounds`.
- `KoogAgentEventSink.onToolCallStarting` reuses an open entry with the same provider id. Without
  that, a resumed call appears twice: once hydrated from history, once from the live event. A
  completed entry with the same id is not reused, so a provider that recycles ids still gets a new
  row.
- The Koog runner re-hydrates the timeline in its `finally`; the echo runner used by pipeline tests
  commits nothing, which is why the refresh is not in the executor.
- The mapper derives the pending turn from entries, not from a second read of the conversation:
  the highest anchored `messageSequence`, no `UserMessage` in it, every call unanswered, and no
  entry with a null anchor (a live run's). The key by position, `call-N`, counts only the message's
  tool calls so entries and parts agree.
- Only a gated turn is resumed on decisions. A free tool's dangling call stays a dead run: resuming
  it blindly would hand a budget-exhausted turn a fresh budget every launch.

## Tool API

`tool-api` depends on neither Koog nor `common:agent:api`: `ChatToolContext.chatId` is a `String`.
Tool providers are invoked per run, never at startup. Deleting a chat does not cancel its run; the
run finishes against storage that no longer accepts it and is discarded.

## Test harness

- `TestAgentModule` runs the real module over in-memory fakes. Neither fake reimplements Room's
  logic: `InMemoryChatHistoryRepository` stores whole conversations with no diff, and
  `InMemoryChatMetadataRepository` matches titles only and returns no snippet. Room mappers, codecs and
  the DAO's commit transaction are covered only by their own unit tests or by hand.
- `StubLLMClient` scripts the model's replies but cannot fail one turn and succeed the next; write
  such conversations by hand with `commitMessages`.
- In `runTest` harnesses, launch the agent scope and collectors as `CoroutineScope(coroutineContext +
  Job())` children of the test scope, not `backgroundScope`: `advanceUntilIdle()` does not advance
  `backgroundScope` coroutines.
- The tombstone lands after the commit, so "no checkpoint rows" is the wrong assertion after a run;
  "nothing restorable" is the right one.
