# TODO

Outstanding work on the agent + database subsystems, as of the last session.

Everything listed here compiles on **both** Android and iOS, and all tests pass — these are gaps in
*functionality and verification*, not broken code.

Current state: the agent runs as a Koog graph with chat memory and persistence installed, proven
against a scripted model. Conversations and checkpoints are Room-backed; the timeline and run queue
are in-memory projections rebuilt from them.

**The app now launches and hosts real screens.** Startup runs from `App.kt`, a Circuit graph routes
between a chats list and the chat screen, and the database opens for the first time when the list
loads. Everything below is what remains.

---

## 1. Startup and hosting — done

Recorded rather than deleted, because the shape of it is not obvious from the code.

- [ ] `AppOnCreateAction.tag` is unread today (only `BaseInitializer.tag` is logged, in
      `FeatureRegistry`). Kept for tracing later; do not remove.

### 1.1 Startup runs from `App.kt`, not from an `Application` class

`rememberAppStartup()` is an `expect`/`actual` composable in `shared/.../startup/`. The Android
actual installs the Room `Context` from `LocalContext.current` and then starts; the iOS actual only
starts, because its `DatabaseBuilderFactory` resolves `NSDocumentDirectory` itself. `AppStartup`
guards with a flag, then runs `FeatureRegistry().initialize()` followed by
`AppOnCreateActionsCollector.execute()` — in that order, since the actions resolve the holders the
initializers register.

There is deliberately **no `Application` subclass**: both platforms enter through Compose, so this is
the only place they genuinely share.

- [ ] `execute()` builds the Room database object on the main thread (Room opens the file lazily on
      first query, and those run in `ProcessScope`). If launch ever feels janky, this is the line
- [ ] `App()` is no longer `@Preview`-able — it installs a context and initialises features. Preview
      individual screens instead

### 1.2 Android manifest

`INTERNET` is now declared. `android:usesCleartextTraffic="true"` is also set so the debug seeder can
point at a local LLM server over plain HTTP.

- [ ] **Remove `usesCleartextTraffic` before any release build.** It is debug convenience only

---

## 2. Agent runtime

### 2.1 Koog runner — done, but never exercised against a real endpoint

`KoogAgentRunner` + `KoogClientFactory` are implemented and selected per configuration by
`AgentRunnerFactory`. With no configuration the run fails visibly rather than answering — an echo
stub would present a setup problem as a working agent.

- [ ] **No request has ever been sent.** Verify against a real endpoint (OpenAI, OpenRouter, or a
      local LM Studio / Ollama server) — streaming, history round-trip, and error paths
- [ ] Tools and planning strategies: the runner is a plain streaming exchange with neither

### 2.1a A crash inside a tool leaves its side effect unrecorded

Checkpoints are written after a node completes (`Persistence.kt:146`). The tool loop put a node
boundary either side of every tool execution, so a crash now keeps every completed step and loses
only the node in flight — where before, with one node, it lost the whole run's visible output. That
was the goal of §2.1a and it is met.

What remains is narrower. A crash *inside* `executeTools` can perform a tool's side effect and
record nothing, so a resumed turn re-issues it:

- The window is milliseconds, against streaming nodes that run for tens of seconds.
- Only `write_file` and `edit_file` have side effects, both inside one chat's sandbox.
- `write_file` re-run with the same content is harmless. `edit_file` is not idempotent when
  `new_text` contains `old_text` — the shape of every "append under this heading" edit — and its
  absent/ambiguous checks both pass, so the text lands twice.

**Deliberately not fixed.** `edit_file` returns the edited region instead, so a double-apply is
legible in the text the model reads back and it can repair it, rather than sitting silently in the
file. A real guard (refusing when the file already contains `new_text`) is heuristic and would block
a legitimate repeated edit. Fixing the ordering properly means journalling intent before dispatch,
which duplicates Koog's persistence.

- [ ] Revisit if a tool with a side effect outside the sandbox is ever added — that changes the
      calculation entirely
- [x] `AgentRunCoordinator` resumes an interrupted tool turn rather than stranding it; see §2.4

### 2.1b The OpenAI-compatible path silently discards reasoning

Only `AgentConfig.Ollama` can produce a thought bubble today. This is a Koog limitation, recorded so
it is not re-derived: `OpenAIStreamDelta` (`prompt-executor-openai-client-base`,
`OpenAIDataModels.kt:956`) declares only `content`, `refusal`, `role` and `toolCalls`, and
`OpenAILLMClient.processStreamingResponse` emits nothing but text and tool-call deltas. Every
`StreamFrame.ReasoningDelta` in that artifact belongs to the **Responses API** branch, which
`LLMCapability.OpenAIEndpoint.Completions` deliberately avoids. So a provider that reports thinking
over `/v1` — Ollama's shim spells it `reasoning`, DeepSeek's `reasoning_content` — has it dropped at
parse time, before any of our code runs.

- [ ] Revisit when Koog adds the field; the timeline below the client already handles it
- [ ] `AgentConfig.Anthropic` has never been run at all, so whether Anthropic reasoning arrives is
      unknown rather than known-broken

### 2.1c Koog's Ollama client drops tools when it streams

`OllamaClient.executeStreaming` accepts `tools: List<ToolDescriptor>` and then builds its
`OllamaChatRequestDTO` **without a `tools` field** (`OllamaClient.kt:303-311`), while its own
non-streaming path at line 215 includes it. Our runner streams, so on an Ollama config the model
would never be told a tool exists — and would simply answer without one, with nothing logged.
Verified identical in Koog 1.2.0, so an upgrade does not fix it.

`ToolForwardingOllamaClient` routes around it: an `LLMClient` decorator, following Koog's own
`RetryingLLMClient`, that moves the tools it was handed into `params.additionalProperties` — the one
channel the delegate forwards, since `OllamaChatRequestDTOSerializer` merges those into the request
root. Keeping it at the client boundary is the point: `KoogTarget`, the runner, the registry and the
loop stay provider-agnostic.

- [ ] **Delete the decorator** when Koog forwards tools on the streaming path
- [ ] It fails silently if Koog changes underneath it — a model told about no tools just answers
      without them, so only a manual check catches it

### 2.2 API keys are stored in plaintext

Connection profiles live in `agent_config.payloadJson`, API key included. On Android this is
app-private storage — a reasonable baseline, but not the Keystore. `AgentConfig.Ollama` is exempt:
it holds no credential at all.

**Now reachable from the app**, which raises the stakes: the configuration screen accepts an API key
by hand, so a real key can be entered without editing source. It goes into the same plaintext column.

- [ ] Add an `expect`/`actual` `SecretStore` (Android Keystore / iOS Keychain) and store only a
      ciphertext blob or alias in the row
- [ ] Until then, treat a device backup or a rooted device as key exposure

### 2.3 Tools — done

`feature:agent-tools` contributes six: `list_files`, `read_file`, `write_file`, `edit_file`,
`fetch_url` and `web_search`. They reach the runner through `common:agent:tool-api`, a Koog-free
contract, so a feature that contributes a tool never compiles against the framework — the separate
module §2.3 originally called for, rather than widening `common:agent:api`.

`KoogAgentRunner` runs a real loop now: `respond → executeTools → sendToolResults → …`,
ending when a reply carries no tool calls, bounded by a per-run budget.

- [x] **Tool approval** (September 2026). Every file and web tool sets `requiresApproval`; a call
      to one ends the run with the turn committed, shows as its own item in the chat, and the user
      allows it once, for the chat, or everywhere, or declines it. Decisions are in memory, rules in
      `agent_tool_approval_rule`, and the run resumes straight into the tool calls once every call
      in the turn is decided. The design is in `docs/architecture/AGENT_RUNTIME.md` (Tool approval).
      Suspending inside the tool was rejected: it would hold the chat's worker for as long as the
      user takes, and a killed process would lose the turn instead of re-asking
- [ ] `requiresApprovalPerValue` is wired end to end but no parameter sets it yet; `fetch_url.url`
      is the obvious first
- [ ] Nothing shows in the chats list that a chat is waiting on an approval; only the chat screen
      knows

- [ ] Deleting a chat does not delete its files. The fix needs a cleanup hook on `tool-api`, because
      the alternative is `common:agent:impl` depending on a feature module
- [ ] `web_search` scrapes DuckDuckGo's HTML endpoint. It will break without warning when the markup
      changes; the tool fails loudly rather than reporting "no results", which is the only reason
      that is survivable
- [ ] Nothing rate-limits or budgets tool calls across a conversation, only within one turn
- [ ] **A chat keeps the system prompt it was created with.** `ChatMemory` replaces the prompt with
      restored history rather than merging into it, so `AIAgent.builder().systemPrompt(...)` only
      applies on a chat's first run; after that the stored copy is what the model sees. Editing
      `AGENT_SYSTEM_PROMPT` therefore reaches new chats only. Fixing it means swapping the stored
      system message for the current one in `PersistentChatHistoryProvider.load` — worth doing
      before the prompt ships to anyone, not worth it while it changes hourly

### 2.4 Process-death recovery — partially done

`AgentRunCoordinator.start()` re-queues prompts whose durable conversation ends unanswered. Chats
holding a live checkpoint are deliberately left alone: auto-resuming every one would retry-loop on a
run that fails for a permanent reason (a bad API key would re-fire on each launch).

- [ ] Optional: resume from a checkpoint via `Persistence.Feature.runFromCheckpoint`, using Koog's
      tombstones to tell "killed" from "failed" so a doomed run is not retried forever
- [x] The `AppOnCreateAction` registered by `ThoonAgentInitializer` now runs at startup, so recovery
      fires without waiting for something to touch the holder.
- [ ] A **free tool's call left unanswered** by a dead run is still not resumed, now on purpose: a
      budget-exhausted turn can also end with unexecuted calls, and resuming those blindly would hand
      the model a fresh budget every launch. Gated turns are resumed only on the user's decisions
- [x] An **interrupted tool turn** is recovered too. The loop created a third way a conversation can
      end — a user-role message carrying `Tool.Result` parts — which `unansweredPrompt` cannot see,
      because that message has no text. Such a chat used to look finished and stall forever.
      `hasUnfinishedToolTurn` detects it and the coordinator resumes it with a *blank* prompt, since
      the conversation already holds everything the model needs.

---

## 3. Database

### 3.1 It opens now, but no test exercises it

Room's KSP validates every query against the schema at build time, so the SQL is not merely
hopeful — but **no test has executed a single statement**. `RoomChatHistoryRepository`,
`RoomCheckpointRepository`, `RoomChatMetadataRepository` and `RoomAgentConfigRepository` have never
run; every test uses in-memory equivalents.

The app itself now opens it — the chats list reads `agent_chat` on launch, so the Room actuals are
no longer purely theoretical. **No automated test still executes SQL**, so a regression would only
surface by running the app.

- [ ] Add instrumented / simulator tests that open `ThoonDatabase` and exercise the repositories
- [ ] These must live in `:shared` — feature modules cannot build the database, because it sits
      downstream of them (it has to see their entities)
- [ ] `ChatDao.deleteChat` is the one hand-written `@Transaction` (row + checkpoints, since
      `agent_checkpoint` has no FK cascade). It has never been executed under test

### 3.2 Migrations

- [ ] `ThoonMigrations.ALL` is empty. Every feature's migration goes here; the version is shared
- [ ] `applyThoonDefaults` deliberately omits `fallbackToDestructiveMigration` — with one file, a
      destructive fallback wipes every feature's data. Keep it that way; add real migrations

### 3.3 Known trade-offs of the single-database design

Recorded so they aren't rediscovered as bugs:

- Entities and DAOs in feature modules must be `public` (`@Database(entities = […])` in `:shared`
  cannot see `internal` types). Repositories, mappers and use cases remain `internal`
- Any feature's schema change bumps the shared version and recompiles `:shared`
- A DAO added without a matching registration in `DatabaseInitializer` fails at *runtime*.
  `DatabaseRegistrationTest` in `:shared` guards this — **add new DAOs to its `ALL_DAOS` set**

---

## 4. Chat feature

- [ ] **No settings UI exists.** The configuration use cases support create/edit/remove/observe, a
      global default and per-chat overrides, but the only thing calling them is
      `DebugAgentConfigSeeder`, driven by a button on the chats list. Its API key is a hardcoded
      constant that must be edited by hand. A `:feature:settings` Circuit screen replaces both
- [ ] Submit is bound to the keyboard's Send action (`ImeAction.Send` + `onKeyboardAction`). There
      is no send button — the `PromptSubmitted` event contract is already in place for one
- [ ] `CancelGenerationClicked` is handled by the presenter but no UI element emits it
- [ ] **Nothing ever names a chat.** `ChatMetadataRepository.setTitle` exists and has no callers, so
      every `ChatSummary.title` is null and the list falls back to `"Chat <id prefix>"`. Deriving a
      title from the first prompt is the obvious fix
- [ ] The chat screen's own title is hardcoded to `"Thoon"`
- [ ] Attachments: `ChatItem.Message.attachments` is always `persistentListOf()` — the agent api's
      `ChatItem` has no attachment concept yet
- [ ] Reasoning → `Thought` mapping splits the agent's single reasoning stream on blank lines,
      which is a heuristic, not a real structure

---

## 4a. Chats list — deliberately primitive

`feature/chats-list` exists to make the app testable, not to be the real screen. It lists chats
newest-first, opens one, creates one, deletes one, and seeds a debug configuration.

- [x] **The feature-to-feature coupling is gone.** Both features are split into `api` (one `Screen`,
      the navigation key) and `impl` (everything else), so `chats-list:impl` depends on
      `:feature:chat:api` and nothing more. See §7.
- [x] Rows show the last thing said, or the matching text when the row came from a search, and a
      chat is named after its first prompt — `ChatMetadataRepository.setTitle` finally has a caller
- [ ] Nothing shows *when* a chat last spoke. `lastMessageAt` is stored and sorted on but never
      rendered; a relative formatter is the missing piece
- [ ] Delete is still a trash icon on every row. Swipe-to-dismiss suits the redesign better
- [ ] Deleting a chat does **not** cancel a run already executing for it. The run finishes against a
      row that no longer exists (`commitHistory` is an `UPDATE`, so it is a harmless no-op) but the
      work is wasted. Proper cancellation belongs with `AgentRunCoordinator`
- [ ] No confirmation on delete, no empty-state design, no pagination

---

## 4b. Conversations are rows, not a blob — done

`historyJson` is gone. A conversation is `agent_chat_message` (one row per turn: role and metadata)
and `agent_chat_part` (one row per text, thought, tool call or result). Each part stores its own
serialised `MessagePart`, so everything the row model does not name — encrypted reasoning, attachment
sources, cache control — round-trips through the framework's serialiser rather than through a mapping
of ours. `KoogMessageRowCodecTest` pins the round trip against all four shapes that occur in
practice.

Why it changed, measured on a real device database before the work: one 20-message chat held
**47,275 bytes**, of which **40% was the model's private reasoning** and 12% tool traffic. Every
metadata read was `SELECT *`, so the chats list loaded all of it to render a title and a timestamp.

Search is now SQL, and correct by construction — verified against that same conversation rebuilt in
the new schema:

| query | old `historyJson LIKE` | new query |
|---|---|---|
| `acceptable`, `address`, `analogy` (reasoning only) | 1 match each | **0** |
| words from `AGENT_SYSTEM_PROMPT` | would match every chat | **0** |
| `snapdragon` (actually said) | 1 | 1, with a snippet |

Two clauses carry that and neither is decoration: `kind = 'text'` keeps reasoning and tool output
out, and `role IN ('user','assistant')` keeps the system prompt out — it is a text part like any
other.

- [ ] `LIKE '%…%'` cannot use an index, so search scans the parts table. Far cheaper than parsing
      every conversation, but still a scan — FTS4 is the upgrade, deferred until it is known whether
      the bundled SQLite ships it on both Android and iOS
- [ ] **Blobs did not fully disappear.** `AgentCheckpointData.messageHistory` is still serialised
      whole by `CheckpointCodec` while a run is in flight; that is the framework's type written by
      its own feature. Only committed conversations are rows
- [ ] `FORMAT_VERSION` is gone with the blob, and with it the clean "discard it, the chat starts
      fresh" escape hatch. An unknown part type now fails a conversation loudly, which is the right
      default but leaves no way to recover the readable half
- [ ] Appending relies on conversations growing at the end. `conversationDiff` verifies that and
      rewrites when it does not, but installing a `ChatMemory` windowing preprocessor would make the
      rewrite path the common one — measure before adding one
- [ ] No migration exists from version 1. Deliberate: the schema changed pre-release and an old
      install fails loudly on open

---

## 5. Verification gaps

### 5.1 iOS compiles; the framework link needs Xcode selected

The konan toolchain problem is fixed. **`iosArm64` and `iosSimulatorArm64` both compile**, across
every module.

Linking a framework still fails, and it is environment rather than code: Kotlin/Native shells out to
`xcrun xcodebuild` to build its compiler caches, and `xcode-select -p` points at
`/Library/Developer/CommandLineTools`, which has no `xcodebuild`. Xcode.app is installed.

- [ ] `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`, then
      `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- [ ] If the atomicfu cache still fails afterwards, `kotlin.native.cacheKind=none` in
      `gradle.properties` sidesteps cache generation (slower builds)
- [ ] Not yet exercised on iOS at all: Room codegen for the iOS targets, and the
      `DatabaseBuilderFactory` actual that resolves `NSDocumentDirectory`

**Make the iOS compile part of the normal loop.** It found two bugs the moment it started working,
both of which had been invisible for a whole session of "the build is green":

- `Dispatchers.IO` as a *member* is JVM-only; the `kotlinx.coroutines.IO` extension exists on
  Native and `common:coroutines` uses it. `common:database` keeps `Dispatchers.Default` on iOS
  because Room serialises its own writes and queries only need to stay off main (see
  `common/database/AGENTS.md`).
- A `listOf(...)` of `LLMCapability` objects inferred as `List<Any>` on the Native backend while
  resolving to `List<LLMCapability>` on the JVM. Same source, same compiler version, different
  inference — so **"it compiles on Android" is not evidence that commonMain is correct**.

The class of bug this catches is the one nothing else does: a symbol that exists only in a
dependency's `jvmCommonMain` (see §6, `HttpClientFactoryResolver`).

### 5.2 Template leftovers — removed

`SharedCommonTest`, `SharedLogicAndroidHostTest`, `SharedLogicIOSTest` (all asserting `3 == 3`) are
gone, along with `Greeting`, `GreetingUtil` and the `Platform` expect/actual that only they used.

- [ ] `feature/chat`'s own `ExampleUnitTest` / `ExampleInstrumentedTest` are still wizard leftovers
- [ ] The generated `compose_multiplatform` drawable is now unreferenced

---

## 5a. The web target needs a second file store

Every module now also builds for `wasmJs` (`:webApp` is the entry point). The file tools run there
over an in-memory okio `FakeFileSystem`, so a browser reload loses every chat's files. A real store
is still the fix, and okio cannot be it — okio publishes `wasmJs` artifacts, yet:

- `FileSystem.SYSTEM` does not exist there; okio's wasm companion declares only
  `SYSTEM_TEMPORARY_DIRECTORY`, because a browser has no system filesystem.
- okio's `FileSystem` API is entirely **synchronous** (`source`, `sink`, `list`), and **OPFS is
  asynchronous**. Its only synchronous door, `createSyncAccessHandle()`, exists solely inside a Web
  Worker, and directory enumeration stays async even there. So `OpfsFileSystem : FileSystem()`
  cannot be written.

This is why the seam is our own `suspend ChatFileStore` rather than okio's `FileSystem`: a web port
writes one more implementation of a four-method interface. The sandbox check lives in commonMain on
okio's `Path`, which *is* available on wasmJs, so the security-critical part is inherited rather
than rewritten.

- [ ] Write the OPFS-backed `ChatFileStore` for web, replacing the in-memory `FakeFileSystem`
- [ ] The network tools cannot work in a browser at all — CORS blocks both `fetch_url` and
      `web_search` against third-party origins

---

## 6. Decisions worth revisiting

- **minSdk is 29** (Android 10). Koog forces ≥ 26 on Android — `ai.koog:agents-core` pulls
  `serialization-jackson` → `jackson-module-kotlin`, which uses `MethodHandle.invokeExact` and
  fails dexing below API 26. Core library desugaring does not help. 29 was chosen deliberately but
  **26 is all that's strictly required**; dropping to 26 would restore Android 8.0–9 support
- **`iosX64`** was removed from the agent modules to match the rest of the repo (only `iosArm64` +
  `iosSimulatorArm64`). Re-add repo-wide if Intel simulators are needed
- **`common:database` casts** Room's builder type parameter to the base type in both actuals.
  Necessary because `Room.databaseBuilder` is `inline reified` and an `expect` member cannot be.
  Sound only because the initializer is always passed explicitly, so Room never takes its
  reflective constructor-lookup path — do not "simplify" by dropping the initializer argument
- **Koog's `HttpClientFactoryResolver` is `jvmCommonMain`-only.** `KoogClientFactory` deliberately
  uses `KtorKoogHttpClient.Factory(baseClient = …)` instead, which is genuinely commonMain. The
  resolver compiles fine on Android and breaks the iOS build, so this is not a simplification
  opportunity. Note the `KoogHttpClient` (non-factory) overload of `OpenAILLMClient` takes **no**
  `apiKey` — auth arrives through `Factory.create(…, headers, …)`, so passing a pre-built client
  would silently drop the key
- **One Ktor client for the app**, from `common:network`. Clients own a connection pool and a
  thread pool, so per-call-site instances leak. Its logging is capped at `LogLevel.INFO` on
  purpose: headers carry API keys and bodies carry conversations
- **`Json` is shared** via `common:serialization` and injected into both codecs. `ignoreUnknownKeys`
  is load-bearing, not cosmetic — Koog's checkpoint serializer migrates older payloads and needs
  tolerant decoding. Dropping that flag breaks checkpoint reads

---

## 7. Feature api/impl and the screen collector — done

Each feature publishes a **direction** from a bare `api` module — one file, depending on
`circuit-runtime`, `:common:navigation`, and `:common:agent:api` when the screen carries a typed id.
Its `impl` registers a `ScreenFactory` *and the screen's serializer* with `ScreenFactoriesCollector`
from its base initializer, keyed by screen type:

```kotlin
ScreenFactoriesCollector.collect(ChatScreen.serializer()) { ChatComponentHolder.get().screenFactory() }
```

**Screens moved from Parcelize to kotlinx-serialization with Circuit 0.39.0** (Circuit 0.38 made
`Screen` non-Parcelable). That removed the Android-only `CommonParcelize` marker and its per-module
compiler flag, gave iOS the same persistence path as Android, and let screens carry `ChatId` and
`AgentConfigId` instead of strings. The serializer rides in the same `collect` call because an
unregistered screen now fails the save instead of being dropped. The toolchain moved with it: Kotlin
2.4.20, Compose Multiplatform 1.12.0, AGP 9.4.0 (which needs Gradle 9.6.0), KSP 2.3.12.

`App.kt` names no feature at all: one `CollectedScreenFactories` is registered with Circuit and
resolves by looking the screen up, so a feature's component — and through it the agent subsystem —
is not built until something navigates to it.

No global navigator was added, deliberately. Circuit's `Navigator` already is one, bound to a back
stack and injected into every presenter; a singleton would have no back stack and would make
`Navigator.NoOp` in the factory tests impossible.

- [ ] **`shared` still names every initializer** in `FeatureRegistry`. That is inherent to having a
      composition root, not debt — but it is the one place that still knows the full feature list
- [ ] A feature that forgets to `collect` renders Circuit's `onUnavailableContent`, not an error.
      `CollectedScreenFactories` logs it; nothing fails the build
- [ ] Screens are matched by **exact class**, where a hand-written Circuit factory matches with
      `is`. Every concrete screen registers itself; a sealed screen hierarchy registered under its
      base type would not resolve
- [ ] `skipPast` — Circuit's decorate-a-factory hook — is unusable with a single dispatcher. Nothing
      used it; the framework itself only ever passes `null`

---

## 8. Configuration screen — done

`feature:agent-configuration` is split api/impl and registers four screens with the collector. The
seeder is gone: `DebugAgentConfigSeeder` and `SeedDebugConfig` are deleted, and the chats list's
"Seed debug config" button is a settings icon.

**One screen renders every settings list.** `AgentConfigurationScreen(section)` — the root is a
section whose rows navigate, so adding a section costs an entry in `ConfigurationSection` and a
branch in `configurationSectionItems`, which an exhaustive `when` refuses to compile without. Items
carry their own lambdas rather than string keys, so the UI is a dumb renderer.

**Model & Provider is not settings-shaped and has its own screens** — `AgentConfigListScreen` and
`AgentConfigEditorScreen`. It is master-detail CRUD over a sealed four-variant type with typed
validation; running it through the generic renderer would have meant a form library inside a settings
model.

**Per-chat configuration reuses the renderer** — a single-select list plus a "Manage models…" link,
reached from the chat's three-dots menu, which until now toggled a boolean nothing rendered.

**The editor is the reference presenter** for `docs/circuit/PRESENTER.md`: one retained
`AgentConfigEditorStateHolder` owning a plain-data `AgentConfigEditorState`
(`Loading | Missing | Editing`) and every transition to it, mapped to a sealed
`Loading | Error | Data` UI state; per-intent events, an in-flight
guard on every write, `isDefault` derived from the observed default rather than held, engines injected
through the factory, and a `presenter.test {}` suite over fakes. `circuit-test` was added for it.

- [ ] **Agent, Tools and Advanced are empty**, and say why. `agent_settings` holds only
      `defaultConfigId`, so an edited system prompt or a switched-off tool has nowhere to live.
      Giving those sections content means columns first
- [ ] `LocalEnginesCollector` is empty because nothing implements an on-device engine. Registering a
      descriptor is not enough on its own — a real engine must also teach `KoogClientFactory` how to
      serve `AgentConfig.Local`, which is where its `error(...)` branch still is
- [ ] The editor loads a configuration once rather than observing it, so two devices editing the same
      row would not see each other. Correct for a form; worth revisiting if configurations ever sync
- [ ] Deleting the default leaves no default, by design (§2.1). The editor does not say so at the
      moment of deletion
