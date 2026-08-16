# TODO

Outstanding work on the agent + database subsystems, as of the last session.

Everything listed here compiles on **both** Android and iOS, and all tests pass — these are gaps in
*functionality and verification*, not broken code.

Current state: the agent runs as a Koog graph with chat memory and persistence installed, proven
against a scripted model. Conversations and checkpoints are Room-backed; the timeline and run queue
are in-memory projections rebuilt from them. The database is fully wired but **has still never been
opened** — no test executes SQL.

---

## 1. Blocking — nothing works in the real app until these are done

### 1.1 App startup is never invoked

`FeatureRegistry` exists and registers both initializers, but nothing calls it. Same for the
on-create actions collector. Until this is wired, no component holder ever gets a provider and the
first `.get()` throws.

- [ ] Call `FeatureRegistry().initialize()` at app start (Android + iOS)
- [ ] Call `AppOnCreateActionsCollector.execute()` after initializers have run
- [ ] Android: create an `Application` class (only `MainActivity.kt` exists today) and register it
      via `android:name` in `androidApp/src/main/AndroidManifest.xml`
- [ ] iOS: same from `MainViewController`

### 1.2 Android database `Context` is never installed

`common:database` deliberately does not capture a `Context` itself.

- [ ] Call `AndroidDatabaseContext.install(this)` from `Application.onCreate`, before any DAO is
      touched. Missing it throws with an explicit message rather than an NPE.

### 1.3 The app UI is still the KMP wizard template

`shared/src/commonMain/.../App.kt` is the generated "Click me!" screen. Circuit and `ChatScreen`
are not reachable from the running app at all.

- [ ] Replace `App.kt` with the real Circuit setup
- [ ] Add `:common:ui`, `:common:navigation`, `:feature:chat` to `shared/build.gradle.kts`
      (currently only `:common:init`, `:common:log`, `:common:database`, `:common:agent:impl`)
- [ ] Wire `ChatPresenter` into a Circuit `Presenter.Factory` / `Ui.Factory`

---

## 2. Agent runtime

### 2.1 Koog runner — done, but never exercised against a real endpoint

`KoogAgentRunner` + `KoogClientFactory` are implemented and selected per configuration by
`AgentRunnerFactory`. With no configuration the run fails visibly rather than answering — an echo
stub would present a setup problem as a working agent.

- [ ] **No request has ever been sent.** Verify against a real endpoint (OpenAI, OpenRouter, or a
      local LM Studio / Ollama `/v1` server) — streaming, history round-trip, and error paths
- [ ] Tools and planning strategies: the runner is a plain streaming exchange with neither

### 2.1a Mid-run tool calls do not survive a crash (needs the tool loop)

Checkpoints are written *after a node completes*, and the graph is a single node today. So a run
killed mid-stream checkpoints nothing — its partial reply, including any tool calls already made, is
lost. Only the prompt survives, because `SendPromptUseCase` makes it durable before the run starts.

This is the one place the design does not yet deliver what it was built for. It resolves itself once
the tool loop exists: `respond → executeTools → sendResults → respond` puts a node boundary either
side of every tool execution, so each one checkpoints as it happens.

- [ ] Build the tool loop, and apply the same `toMessageResponse` → `appendPrompt` → checkpoint
      pattern to *every* streaming node in it, not just the first
- [ ] Then assert the original goal: kill a run after a tool call, reopen, see the tool call

**Notes for whoever extends this.** Koog 1.1.1 does not match the published docs:
- No `SingleLLMPromptExecutor` — use `MultiLLMPromptExecutor(client)`
- `OpenAIClientFactory.openAIClient()` / `AnthropicClientFactory.anthropicClient()` are **JVM-only
  and absent from the iOS klib**; calling them from `commonMain` compiles on Android and breaks the
  iOS build. Use the constructors, with `HttpClientFactoryResolver.resolve()` for the
  non-defaulted `httpClientFactory` argument
- Third-party OpenAI-compatible endpoints need `LLMCapability.OpenAIEndpoint.Completions`, else the
  client targets the Responses API that most of them don't implement
- `StreamFrame.ReasoningDelta.text` is nullable
- If tools are added later, the agent-graph hooks exist: `onLLMStreamingFrameReceived`,
  `onToolCallStarting` / `Completed` / `Failed`, via `AIAgent.builder().install(…).graphStrategy{}`

### 2.2 API keys are stored in plaintext

Connection profiles live in `agent_config.payloadJson`, API key included. On Android this is
app-private storage — a reasonable baseline, but not the Keystore.

- [ ] Add an `expect`/`actual` `SecretStore` (Android Keystore / iOS Keychain) and store only a
      ciphertext blob or alias in the row
- [ ] Until then, treat a device backup or a rooted device as key exposure

### 2.3 Tools

- [ ] `KoogToolRegistryFactory` — never written. Agent tools should depend on other features'
      **api** modules, never the reverse
- [ ] If features ever need to contribute tools dynamically, add a separate `agent-tool-api`
      rather than widening `common:agent:api`

### 2.4 Process-death recovery — partially done

`AgentRunCoordinator.start()` re-queues prompts whose durable conversation ends unanswered. Chats
holding a live checkpoint are deliberately left alone: auto-resuming every one would retry-loop on a
run that fails for a permanent reason (a bad API key would re-fire on each launch).

- [ ] Optional: resume from a checkpoint via `Persistence.Feature.runFromCheckpoint`, using Koog's
      tombstones to tell "killed" from "failed" so a doomed run is not retried forever
- [ ] The runtime still starts lazily: `ApiComponentHolder` builds on first access, so recovery only
      runs once something touches the holder. The `AppOnCreateAction` registered by
      `ThoonAgentInitializer` covers this *if* §1.1 is done.

---

## 3. Database

### 3.1 It has never actually been opened

Room's KSP validates every query against the schema at build time, so the SQL is not merely
hopeful — but **no test has executed a single statement**. `RoomChatHistoryRepository`,
`RoomCheckpointRepository`, `RoomChatMetadataRepository` and `RoomAgentConfigRepository` have never
run; every test uses in-memory equivalents.

- [ ] Add instrumented / simulator tests that open `ThoonDatabase` and exercise the repositories
- [ ] These must live in `:shared` — feature modules cannot build the database, because it sits
      downstream of them (it has to see their entities)

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
      global default and per-chat overrides, but nothing calls them — configurations can only be
      created from code. A `:feature:settings` Circuit screen is the missing piece, and until it
      exists every prompt fails with "No agent configuration selected"
- [ ] `ChatScreen` is a `data object` with no `ChatId`, so `ChatPresenter` creates a fresh chat on
      every composition. Multiple persisted chats need it to become a `data class` carrying a
      `ChatId`
- [ ] Submit is bound to the keyboard's Send action (`ImeAction.Send` + `onKeyboardAction`). There
      is no send button — the `PromptSubmitted` event contract is already in place for one
- [ ] `CancelGenerationClicked` is handled by the presenter but no UI element emits it
- [ ] `ChatScreen.Event.Ui.GoBackClicked` is a no-op in the presenter (needs navigation)
- [ ] Chat title is hardcoded to `"Thoon"`
- [ ] Attachments: `ChatItem.Message.attachments` is always `persistentListOf()` — the agent api's
      `ChatItem` has no attachment concept yet
- [ ] Reasoning → `Thought` mapping splits the agent's single reasoning stream on blank lines,
      which is a heuristic, not a real structure

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

- `Dispatchers.IO` is `internal` on Native — JVM/Android only. `common:database` now uses
  `Dispatchers.Default` there.
- A `listOf(...)` of `LLMCapability` objects inferred as `List<Any>` on the Native backend while
  resolving to `List<LLMCapability>` on the JVM. Same source, same compiler version, different
  inference — so **"it compiles on Android" is not evidence that commonMain is correct**.

The class of bug this catches is the one nothing else does: a symbol that exists only in a
dependency's `jvmCommonMain` (see §6, `HttpClientFactoryResolver`).

### 5.2 Template tests still present

- [ ] `shared/src/commonTest/.../SharedCommonTest.kt`, `SharedLogicAndroidHostTest.kt`,
      `SharedLogicIOSTest.kt` are wizard leftovers asserting `3 == 3`

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
