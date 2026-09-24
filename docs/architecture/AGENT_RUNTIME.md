# The agent runtime

How a prompt becomes an answer, and what survives a dead process. Module-specific decisions and
Koog quirks are in `common/agent/AGENTS.md`; the tools in `feature/agent-tools/AGENTS.md`.

## Shape

`common:agent:api` exposes **use cases, not repositories**: one operation per interface, so a screen
depends on the operations it performs. Everything behind them (`ChatRepository`,
`ConversationRepository`, Room, Koog) is `internal` to `common:agent:impl`, laid out as
`domain/` (entities, repository interfaces, use cases) → `data/` (Room, in-memory) → `koog/`
(everything that knows the framework) → `execution/` (the run queue).

`domain/` and `data/` never see a Koog type. `StoredMessage` and `StoredPart`, with opaque
`metaInfoJson` and `payloadJson` strings, are the boundary; only the codecs in `koog/` convert.
`impl` keeps its own entities beside the api models (`ChatEntry` vs `ChatItem`, `ToolCallStatus` vs
`ToolCallState`) so run ids, timestamps and raw payloads never leak into the api.

## A prompt's life

1. `SendPromptUseCase` writes the prompt to the conversation **before** enqueuing a run. Dying after
   this leaves work recovery can see; dying before loses an unrecorded keystroke. The two writes are
   separate transactions, not one. `SendPrompt` returns when the prompt is durable, not when it is
   answered; the answer arrives through the observed chat state.
2. `AgentRunCoordinator` observes pending runs and drains them, **one worker per chat**, looping
   through every queued run for a chat so a prompt queued mid-run needs no second notification.
   The run queue (`AgentRunRepository`) lives in process memory and dies with the process. What
   survives is the conversation.
3. `AgentRunExecutor` resolves the config once, when the run starts (per-chat override, else the
   global default), and runs under the chat's lock. Every exit, including cancellation, settles the
   streaming entries so the timeline never shows a run that no longer exists.
4. `KoogAgentRunner` builds a Koog graph: `respond → executeTools → sendToolResults → …`, with
   `executeDecided → sendToolResults` entered directly for a turn the user has decided on. Edge
   order is load-bearing — a turn carrying both text and a tool call must take the tool edge, and
   `AIAgentNode` takes the *first* accepting edge. Koog's `ChatMemory` restores the conversation at
   run start and commits it at run end; `Persistence` checkpoints per node. Both reach Room through
   `PersistentChatHistoryProvider` and `RoomPersistenceStorageProvider`, keyed by the chat id, which
   is also Koog's session id.
5. On completion `store` commits the conversation and `ChatDao.commitConversation` drops that chat's
   checkpoints **in the same transaction**. A checkpoint outliving its commit would replay a landed
   turn. Koog hands `store` the whole conversation every turn; `conversationDiff` decides whether
   the commit appends the turns past the stored boundary or rewrites from zero.

## What a chat is waiting on

`RetryChatUseCase` defines it: an unanswered prompt, a turn interrupted after its tools ran, or a
turn stopped for approval whose every call now has a decision. A tool result is a user-role message
with no text, so the unanswered-prompt check alone would read a conversation ending in tool results
as nothing pending; `hasUnfinishedToolTurn` covers that case, and `pendingToolCalls` the third.
Startup recovery (`StartAgentRuntimeUseCase`, run once from `ThoonAgentInitializer`), the retry
button and the approve button all call it, so they cannot disagree. Both tool cases are resumed by
enqueuing a **blank** prompt: the conversation already ends with what the model has not read, and
appending anything would put a phantom user message in the transcript. A free tool's call left
unanswered by a dead run is *not* resumed: that is a crash to retry by hand, not a wait.

## Tool approval

A tool whose spec sets `requiresApproval` does not run until the user allows it. Nothing suspends
inside the tool. A **pending turn** is a conversation whose last message is an assistant message
carrying tool calls with no results; before this feature only a dead run left one, now it is also
how a run ends when a call needs a decision. The pieces:

- `ToolApprovalResolver` gives every call of a pending turn one verdict: *allowed* (not gated, or a
  stored rule matches), *approved* or *declined* (the user answered this call), or *undecided*. The
  graph, `RetryChatUseCase` and the timeline all ask it, which is what keeps them from disagreeing.
- The graph's tool edges accept a turn only when every call may execute; otherwise the turn falls
  through to the unconditional finish edge and the run ends **normally**. That matters: `ChatMemory`
  commits the turn and `Persistence` writes its tombstone, so the pending call is durable history
  and the next run starts fresh. The edges out of `nodeStart` then take a blank prompt over a
  decided pending turn straight to `executeDecided`, which runs the approved calls and answers the
  declined ones with an error result reading "Declined by the user.", all in one user message, as
  every provider requires. The model never sees an approval; it sees results.
- Decisions live in `ToolApprovalDecisions`, in memory, like the run queue they feed; the executor
  clears a chat's decisions on every exit. The safe failure for a lost decision is to ask again.
  "Always" rules live in Room (`agent_tool_approval_rule`), global or per chat, optionally bound to
  the values of parameters marked `requiresApprovalPerValue`; an empty parameter map is a blanket
  rule, which is what a settings toggle makes.
- The conversation stays single-writer: only the run writes tool results, declined ones included.
- The timeline derives the pending turn from the entries themselves (the highest anchored turn,
  all calls unanswered, no live run) and asks the resolver, so a gated undecided call shows as
  `AwaitingApproval` followed by a `ChatItem.ToolApprovalRequest`, a free call in the same turn as
  `Pending`, and the chat as `ChatExecutionState.AwaitingApproval`. The chat screen blocks the prompt
  field in that state. The tool call key is the provider's id, or `call-N` by position when the
  provider sends none.
- After every run the Koog runner re-hydrates the timeline from what was committed, so the live
  streaming entries are replaced by anchored ones and a turn that stopped for approval shows its
  pending call at once. The sink reuses an existing open entry for a call it starts, or the resumed
  call would appear twice.

## Checkpoints

Checkpoints are a per-node write-ahead journal. The timeline and the model's replay read the same
source: the latest *live* checkpoint if a run left one, else committed history, so what the user sees
and what the model is replayed cannot diverge, and a run that died after its tool calls still shows
them. Koog writes a tombstone checkpoint when a run ends, after `store`; it carries no history and
every reader filters it out. `RoomCheckpointRepository` retains ten per chat only as a safety net.
The shared `Json` must keep `ignoreUnknownKeys = true`: Koog's checkpoint serializer relies on
tolerant decoding.

## Projections

The timeline and the run queue are process-scoped projections over `InMemoryAgentStore`, rebuilt
from storage: `ObserveChatUseCase` hydrates from `ConversationRepository.timeline` on every
subscribe. There is no Room-backed `ChatRepository` and none is planned; leaving a chat mid-answer
loses nothing because the run, not the screen, owns the work.

## Things that cost time to rediscover

- `ChatMemory` **replaces** the prompt with restored history rather than merging. The system message
  must therefore be restored with everything else, or every turn after the first has no system
  prompt. Corollary: editing `AGENT_SYSTEM_PROMPT` reaches new chats only. It is also why the
  current date is a tool and not a line in the prompt: a date in the prompt would freeze at the
  chat's first turn.
- Koog's reflective tool API (`ToolSet`, `@Tool`, `asTools()`), `openAIClient()` and
  `HttpClientFactoryResolver` live in `jvmCommonMain` and are absent from the iOS klib. They compile
  on Android and break the iOS build. `KoogToolRegistryFactory` writes descriptors by hand for
  exactly this reason.
- Koog's `OllamaClient.executeStreaming` accepts `tools` and omits them from the request.
  `ToolForwardingOllamaClient` decorates it to put them back.
- The OpenAI `/v1` compatibility layer drops reasoning, which is why `AgentConfig.Ollama` exists as
  its own protocol rather than as an `OpenAiCompatible` pointed at `/v1`.

## Tools

Seven, all in `feature/agent-tools`: `list_files`, `read_file`, `write_file`, `edit_file`,
`fetch_url`, `web_search`, `current_datetime`. All but the last require approval (see Tool
approval above). They implement `ThoonAgentTool` from
`common:agent:tool-api`, a contract that depends on neither Koog nor `common:agent:api`, so a feature
contributing a tool never compiles against the framework. `KoogToolRegistryFactory` is the adapter;
`AgentToolSpec` is not a JSON schema, and `AgentToolParameterType` grows only with types the adapter
can map. Tools are listed in `AgentToolsModule.Impl.agentToolProvider`; **adding one is one class and
one line in that list.** Providers are invoked per run, never at startup, so nothing in a provider may
be built in an initializer. Zero-parameter tools are fine. Throw anything but
`CancellationException` to report failure: the message reaches the model as the result, so write it
for that reader. Tool names are stored in conversations,
so renaming one orphans past calls; descriptions are the model's only guidance on when to call.

`web_search` scrapes DuckDuckGo's HTML endpoint, and three things about it are not obvious:

- An empty parse has three causes and the tool tells them apart: a page marked `no-results` (an
  ordinary answer — the model should rephrase), the anti-bot interstitial (this device is blocked),
  or the markup having moved (the tool is broken). Reporting the first as the third once sent the
  model hunting for a fault that was not there.
- **The interstitial is served as HTTP 202**, which `isSuccess()` accepts. Check the body first.
- The emulator shares the host's IP. Hammering the endpoint from a terminal to test it gets the
  *app* blocked too. Test against saved fixture HTML (`WebSearchToolTest`, via Ktor `MockEngine`).
