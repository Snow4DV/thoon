# AGENTS.md

Orientation for an agent working in this repo. Facts here were read out of the code, not inferred.
Where something is a *decision* rather than a fact, it says so.

## What Thoon is

A Kotlin Multiplatform AI-agent app — chats with a model that can call tools, running on Android and
iOS from one Compose codebase. The model is reached over the network (OpenAI-compatible, Anthropic,
or Ollama's native protocol); on-device inference is stubbed but not implemented.

The distinguishing design choice: **a run outlives the screen that started it.** Prompts become
durable rows, a process-scoped coordinator drains them, and the timeline is a projection rebuilt
from storage. Leaving a chat mid-answer loses nothing; killing the app loses at most an uncommitted
turn, which is recovered on next launch.

## Build and test

```bash
./gradlew :androidApp:assembleDebug          # Android app
./gradlew testAndroidHostTest                # every module's host tests (175 today)
./gradlew compileKotlinIosSimulatorArm64 compileKotlinIosArm64   # iOS must keep compiling
```

Run all of these before calling anything done. **iOS is a real target, not aspirational** —
`commonMain` code that only compiles on Android is a broken build, and the compile tasks are the
cheapest way to find out.

- iOS *tests* (`iosSimulatorArm64Test`) need Xcode selected:
  `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`. Compiling does not.
- Local model for development: Ollama on the host, reached from the emulator as
  `http://10.0.2.2:11434` (`AgentConfig.Ollama`; `localhost` on the emulator is the emulator).
  `normalizeBaseUrl` turns a bare private/loopback address into `http://` and anything routable
  into `https://`, so that address is accepted without a scheme.
- Inspecting the database: `adb shell run-as com.mvlog.thoon cat databases/thoon.db` — **and
  `thoon.db-wal` and `thoon.db-shm`**; recent writes live in the WAL and a pull of the main file
  alone shows stale data. Pull into the scratchpad, query with `sqlite3`, and **delete the copies
  afterwards** — they hold the owner's conversations. This is also the only way the SQL is verified
  (see Testing).
- `README.md` is the stale KMP template. Ignore it. `TODO.md` is real and current.
- **Confirm a new test actually ran.** Gradle prints `BUILD SUCCESSFUL` when a test file was never
  written (a heredoc into a directory that does not exist fails silently) or was filtered out. Check
  `<module>/build/test-results/testAndroidHostTest/TEST-<class>.xml` for `tests="N"`, or use
  `--rerun-tasks`. This has bitten before.
- macOS: there is no `timeout` command; a pipeline starting with it silently runs nothing.
- Before adding a dependency, look in `~/.gradle/caches/modules-2/files-2.1/` — several libraries
  (kotlinx-datetime among them, until it was made explicit) are already resolved transitively for
  all three targets, which is what makes adding them low-risk.

**Git history is not a guide here.** Two commits exist and most of the tree is uncommitted, so
`git log` tells you nothing about why code looks the way it does. The KDoc does — see Conventions.

## Module map

```
androidApp/            Android entry point (MainActivity)
iosApp/                Xcode project
shared/                composition root: App.kt, ThoonDatabase, FeatureRegistry, startup
common/
  agent/api            use cases + models the app talks to (no Koog types)
  agent/impl           agent runtime, storage, Koog integration
  agent/tool-api       tool contract, Koog-free, so features can add tools
  navigation           Screen factory collector, CommonParcelize
  database             DaoFactory, database builder (expect/actual)
  di  init             ComponentHolder / BaseInitializer plumbing
  ui  markdown  log  coroutines  network  serialization
feature/
  chat/{api,impl}                 the conversation screen
  chats-list/{api,impl}           list, search, bottom bar
  agent-configuration/{api,impl}  model & provider settings, per-chat overrides
  agent-tools                     file, web and date/time tools (one module — no screen)
```

**`api` holds `Screen`s and contracts; `impl` holds presenters, UI and DI.** A feature depends on
another feature's `api` only, so nothing depends on another feature's implementation.

## Architecture

### Layers

`common:agent:api` exposes **use cases, not repositories** — a screen depends on the operations it
performs. Everything behind them (`ChatRepository`, `ConversationRepository`, Room, Koog) is
`internal` to `common:agent:impl`.

Inside `agent/impl`: `domain/` (entities, repository interfaces, use cases) → `data/` (Room, in-memory)
→ `koog/` (everything that knows the framework) → `execution/` (the run queue).

### DI — hand-rolled, no framework

Each module follows one template. For a feature named `Foo`, `feature/foo/impl/di/` holds six files:

| File | Role |
| --- | --- |
| `FooComponent` | what the feature exposes |
| `FooComponentDependencies` | what it needs, each defaulted to a holder lookup |
| `FooModule` | the graph; `Impl(dependencies) : FooModule, FooComponentDependencies by dependencies` |
| `FooComponentImpl` | `FooComponent` backed by the module |
| `FooComponentHolder` | the singleton access point |
| `FooInitializer` | registers the holder and any collectors |

Dependencies are satisfied by *delegation to another module's holder*, resolved per access:

```kotlin
internal interface ChatComponentDependencies {
    val retryChatUseCase: RetryChatUseCase
        get() = ThoonAgentComponentHolder.get().retryChatUseCase()

    class Impl : ChatComponentDependencies
}

internal interface ChatModule {
    val screenFactory: ScreenFactory

    // `by dependencies` is what lets provider bodies read as bare names.
    class Impl(dependencies: ChatComponentDependencies) :
        ChatModule, ChatComponentDependencies by dependencies { /* ... */ }
}

internal class ChatComponentImpl(
    // Both defaulted — this is the test seam.
    dependencies: ChatComponentDependencies = ChatComponentDependencies.Impl(),
    private val module: ChatModule = ChatModule.Impl(dependencies),
) : ChatComponent

internal object ChatComponentHolder : ApiComponentHolder<ChatComponent>()

// The module's only public DI type.
class ChatInitializer : BaseInitializer(tag = TAG) {
    override fun init() {
        ChatComponentHolder.set { ChatComponentImpl() }
        ScreenFactoriesCollector.collect<ChatScreen> { ChatComponentHolder.get().screenFactory() }
    }
}
```

**Every DI type is `internal` except the initializer** — that is what stops another module reaching
past the holder. Providers are `get()` properties (fresh each access) unless something must be a
singleton — a coordinator owning live jobs, a mutex registry — which is `by lazy` with a comment
saying why. `ApiComponentHolder.get()` caches on first call; `reset()` (tests only) clears the
instance but keeps the provider.

### Startup order

There is **no `Application` subclass**: both platforms enter through Compose, so `App()` is the only
shared entry point. `rememberAppStartup()` is `expect`/`actual` — the Android actual installs the
database and files `Context` first; iOS resolves its own directory. Then, in this order:

1. `FeatureRegistry().initialize()` — every `BaseInitializer`, each wrapped in `runCatching` so one
   failure is logged rather than fatal. Order-independent: holders build lazily.
2. `AppOnCreateActionsCollector.execute()` — actions that need the holders the initializers just
   registered. Today there is exactly one: `ThoonAgentInitializer` starts the run coordinator, so
   unfinished work is recovered at launch.

Phase 1 registers *providers* and collects; no component is built. The list within phase 1 is
order-independent, the two phases are not.

**A new feature is invisible until its initializer is added to `FeatureRegistry`.**

### Navigation — Circuit, resolved by collector

`App.kt` names no feature. `ScreenFactoriesCollector` maps a `Screen` type to a lazy factory:

```kotlin
ScreenFactoriesCollector.collect<ChatScreen> { ChatComponentHolder.get().screenFactory() }
```

`CollectedScreenFactories` is one dispatcher registered as *both* `Presenter.Factory` and
`Ui.Factory`. Keying by screen type means a feature's component is not built until something
navigates to it. **Matching is by exact class** — a sealed base does not resolve, so every concrete
screen registers itself. A miss logs `No feature registered a factory for ...` rather than crashing.

`Screen`s live in `feature/*/api` and are `@CommonParcelize` — a plain marker annotation, not an
`expect`/`actual`, because Kotlin 2.0 forbids aliasing annotations that drive a compiler plugin. It
needs **both** the `kotlinParcelize` plugin and this compiler arg, in *every* module that declares or
annotates a `Screen`:

```kotlin
freeCompilerArgs.addAll(
    "-P",
    "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.mvlog.navigation.CommonParcelize",
)
```

Without it the marker is inert and the failure is a lost back stack at runtime, not a compile error.
`@Parcelize` cannot carry value classes, which is why `ChatScreen.chatId` is a `String` and not a
`ChatId`.

### Extension points

Four collectors. Three are **non-draining**; one drains. Getting that wrong is a real bug class.

| Collector | Module | Drains on read? |
| --- | --- | --- |
| `AppOnCreateActionsCollector` | `common:init` | **yes** — actions run once |
| `AgentToolsCollector` | `common:agent:tool-api` | no — the registry is rebuilt per run |
| `ScreenFactoriesCollector` | `common:navigation` | no |
| `LocalEnginesCollector` | `common:agent:api` | no |

Draining `AgentToolsCollector` would give the first run its tools and every later run none — which
looks like the model forgetting its abilities mid-session, not like a DI bug.

### Adding a feature

1. `include(":feature:foo:api")` and `":feature:foo:impl"` in `settings.gradle.kts`.
2. `api` module: the `Screen`(s), `@CommonParcelize`, primitives only in the constructor. Plugins:
   `kotlinMultiplatform`, `androidMultiplatformLibrary`, `androidLint`, `kotlinParcelize` — plus the
   parcelize compiler arg. No Compose plugins in an `api` module.
3. `impl` module: the six DI files, a `Presenter.Factory` and a `Ui.Factory` (both returning `null`
   for screens they do not own — that is the contract, not a fallback), and the UI.
4. In `impl`'s `build.gradle.kts`: `api(project(":common:navigation"))` and
   `api(project(":feature:foo:api"))` — `api` because `ScreenFactory` and the screen type appear in
   the component's signature — then `implementation` for `:common:di`, `:common:init` and any other
   feature's **`api`** module. Never depend on another feature's `impl`.
5. Add `FooInitializer()` to `FeatureRegistry`.

`feature:agent-tools` is the exception to the api/impl split: it contributes tools rather than a
screen, so it is one module and registers into `AgentToolsCollector`.

## The agent runtime

A prompt's life:

1. `SendPromptUseCase` writes the prompt to the conversation **before** enqueuing a run. Dying after
   this leaves work recovery can see; dying before loses an unrecorded keystroke.
2. `AgentRunCoordinator` observes pending runs and drains them, **one worker per chat**.
3. `AgentRunExecutor` resolves the config (per-chat override, else the global default) and runs.
4. `KoogAgentRunner` builds a Koog graph. Edge order is load-bearing — a turn carrying both text and
   a tool call must take the tool edge, and `AIAgentNode` takes the *first* accepting edge.
5. On completion `PersistentChatHistoryProvider.store` commits the conversation and drops that
   chat's checkpoints **in one transaction**. A checkpoint outliving its commit replays a landed turn.

`RetryChatUseCase` defines what a chat is "still waiting on" — an unanswered prompt, or a turn
interrupted after its tools ran. Both startup recovery and the retry button call it, so they cannot
disagree.

Notes that cost time to rediscover:

- `ChatMemory` **replaces** the prompt with restored history rather than merging. The system message
  must therefore be restored with everything else, or every turn after the first has no system prompt.
  **Corollary: editing `AGENT_SYSTEM_PROMPT` reaches new chats only** — an existing chat replays the
  prompt it was born with. It is also why the current date is a tool (`current_datetime`) and not a
  line in the prompt: a date in the prompt would freeze at the chat's first turn.
- **Koog's reflective tool API (`ToolSet`, `@Tool`, `asTools()`) and `openAIClient()` live in
  `jvmCommonMain` and are absent from the iOS klib.** They compile on Android and break the iOS
  build. `KoogToolRegistryFactory` writes descriptors by hand for exactly this reason.
- Koog's `OllamaClient.executeStreaming` accepts `tools` and omits them from the request.
  `ToolForwardingOllamaClient` decorates it to put them back.
- The OpenAI `/v1` compatibility layer drops reasoning, which is why `AgentConfig.Ollama` exists as
  its own protocol rather than as an `OpenAiCompatible` pointed at `/v1`.

### Tools

Seven, all in `feature/agent-tools`: `list_files`, `read_file`, `write_file`, `edit_file`,
`fetch_url`, `web_search`, `current_datetime`. They implement `ThoonAgentTool` from
`common:agent:tool-api` — a Koog-free contract, so a feature contributing a tool never compiles
against the framework — and are listed in `AgentToolsModule.Impl.agentToolProvider`. **Adding one is
one class and one line in that list.** Zero-parameter tools are fine (`list_files`,
`current_datetime` both are). Throw to report failure: the message reaches the model as the result,
so write it for that reader.

`web_search` scrapes DuckDuckGo's HTML endpoint, and three things about it are not obvious:

- An empty parse has three causes and the tool tells them apart: a page marked `no-results` (an
  ordinary answer — the model should rephrase), the anti-bot interstitial (this device is blocked),
  or the markup having moved (the tool is broken). Reporting the first as the third once sent the
  model hunting for a fault that was not there.
- **The interstitial is served as HTTP 202**, which `isSuccess()` accepts. Check the body first.
- The emulator shares the host's IP. Hammering the endpoint from a terminal to test it gets the
  *app* blocked too. Test against saved fixture HTML (`WebSearchToolTest`, via Ktor `MockEngine`).

## Storage

**One Room database for the whole app** (`ThoonDatabase` in `:shared`, currently `VERSION = 2`).

The dependency runs the wrong way on purpose: the database must see every module's entities, so it
lives downstream, and DAOs travel back up through `DaoFactory`:

```kotlin
daoFactory().get<ChatDao>()
```

Consequences, all deliberate:

- Entities and DAOs in feature modules must be **`public`** — `@Database(entities = [...])` in
  `:shared` cannot see `internal` types. Repositories, mappers and use cases stay `internal`.
- Any feature's schema change bumps the shared version.
- **A DAO added without registering it in `DatabaseInitializer` fails at runtime.**
  `DatabaseRegistrationTest` guards this — add new DAOs to its `ALL_DAOS` set.
- `applyThoonDefaults` omits `fallbackToDestructiveMigration` on purpose: with one file, a
  destructive fallback wipes every feature's data. `ThoonMigrations.ALL` is currently empty.

### The conversation schema

Conversations are **rows, not a blob**: `agent_chat_message` (one turn) → `agent_chat_part` (one
piece of content). A turn holds several parts because reasoning and the reply it produced sit side
by side, and only one of them should ever be searched.

- `part.kind` is `text | reasoning | tool_call | tool_result | attachment`. **Search filters to
  `kind = 'text'` and `role IN ('user','assistant')`** — without the first, 52% of a real
  conversation (reasoning + tool traffic) is searchable; without the second, every chat matches any
  word in the system prompt.
- `part.textLower` is lowercased **in Kotlin**, because SQLite folds ASCII only and `LIKE '%привет%'`
  would never match `Привет`.
- `part.payloadJson` is the exact serialised framework part. That is what makes storage lossless —
  encrypted reasoning, attachment sources and cache control round-trip through the framework's own
  serialiser rather than a hand-written mapping.
- `message.createdAt` is **commit time**, stamped once per batch — not the message's own timestamp.
  Never order by it. Order by `coalesce(chat.lastMessageAt, chat.createdAt)`.

### The anchor invariant

`ChatTimelineProjector` assigns entry ids as a counter over *rendered* entries, which cannot be
inverted (system turns are skipped, a user turn collapses, an assistant turn expands per part). So
each entry also carries `messageSequence`, and **that equals `agent_chat_message.sequence`** because
`KoogMessageRowCodec.toRows` writes `sequence = index` and `toMessages` sorts by it. Search deep
links rest entirely on that equality; `ChatTimelineProjectorTest` pins it.

## Testing

What exists to test each layer, and what does not:

| Layer | Use | Where |
| --- | --- | --- |
| Agent graph end to end | `StubLLMClient` — scripts the model's replies, tool calls included | `common/agent/impl/.../commonTest/fake/` |
| Anything needing the DI graph | `TestAgentModule` — the real module over in-memory fakes | same |
| HTTP tools | Ktor `MockEngine` (`ktor-client-mock` is a test dep of `agent-tools`) | `WebSearchToolTest` |
| File tools | okio `FakeFileSystem` | `OkioChatFileStoreTest` |
| Pure logic (codecs, projector, snippets, highlighting) | plain `kotlin.test` | throughout |
| **Room SQL** | **nothing** — no test instantiates `ThoonDatabase` | verify by hand on the emulator |
| **Compose UI behaviour** (scroll, highlight, follow-the-bottom) | **nothing** | verify by hand |

Room does validate every `@Query` at compile time through KSP, so a projection that does not match
its row class fails the build — that catches shape, not semantics. `InMemoryChatMetadataRepository`
matches titles only and returns no snippet; it is deliberately not a second implementation of the
search query.

## Conventions

**Comments explain *why*, never *what*.** This is the strongest convention in the repo and the
easiest to violate. The KDoc carries the reasoning that git history does not: what was tried, what
breaks if it changes, which invariant is load-bearing. Match the surrounding density — a one-line
private helper gets nothing; a query whose `WHERE` clause carries correctness gets a paragraph.

- Kotlin official style, 4 spaces, ~100 column soft wrap.
- Time is `kotlin.time.Instant`/`Clock` from the stdlib (`Clock.System` needs
  `@OptIn(ExperimentalTime::class)`). Civil time — zones, local dates, formatting — is
  `kotlinx-datetime` 0.7.1, whose `toLocalDateTime` extends that same stdlib `Instant`. Only
  `agent-tools` depends on it today; nothing in the UI formats a timestamp yet.
- Tests are `kotlin.test` in `commonTest`, named as sentences:
  `aTurnInterruptedAfterAToolRanIsResumable`. Assertion messages state the consequence
  ("the match must be in the window"), not the mechanics.
- Test fakes live in `commonTest/.../fake/`; `TestAgentModule` wires an in-memory graph.
- Presenters are Circuit `Presenter<UiState>`; state is a sealed `Loading | Error | Data`; events are
  a sealed `UiEvent.Ui`. Presenters hold only presentational state — drafts, expansion — and read
  everything else from storage.
- UI comes from `com.composables.ui` (a third-party design system) plus `common:ui`. Message bodies
  render as markdown via `ThoonMarkdown`. **Feature-specific UI does not go in `common:ui`.**

## Traps

- **Delete-and-reinstall** after a schema change: there are no migrations by design.
- `com.composables.ui.components.Text` has an `AnnotatedString` overload, but `overflow` defaults to
  `TextOverflow.Clip`, not `Ellipsis`.
- The chat list is `reverseLayout = true` over `items.asReversed()` — visual index is
  `items.lastIndex - chronologicalIndex`, and index 0 is the *newest*.
- `snapshotFlow` emits its current value immediately. The chat screen's follow-the-bottom logic
  depends on this; anything trying to pre-set a flag it writes will be overwritten in the first frame.
- Ktor logging is capped at `LogLevel.INFO` deliberately: headers carry API keys and bodies carry
  conversations.

## Security constraints

Currently accepted, and worth not making worse:

- **API keys are stored in plaintext** in `agent_config.payloadJson`. App-private storage is the only
  protection; treat a device backup or a rooted device as key exposure. A `SecretStore`
  (Keystore/Keychain) is the planned fix. `AgentConfig.Ollama` holds no credential.
- **`android:usesCleartextTraffic="true"` is debug convenience** so the emulator can reach a local
  Ollama at `10.0.2.2`. It must not survive into a release build.
- `fetch_url` and `web_search` return untrusted third-party text into a context that can call
  `write_file`. The per-chat file sandbox caps the blast radius at one chat's folder — keep it.

## Working agreements

Decisions the owner has stated explicitly; do not relitigate them, and do not quietly work around them:

- **No migrations while the app is pre-release.** A schema change bumps the version and the owner
  reinstalls. No two-phase rollouts, no compatibility shims, no hanging code for an old shape.
- **Settings and feature UI stay in their feature.** `common:ui` is for primitives every screen
  shares, not for a screen's own rows.
- **A written plan before non-trivial changes**, with the trade-offs stated. The owner reads plans
  and pushes back on them (the flattened-schema search design was rejected on review, correctly);
  a plan that hides a lossy shortcut will be found out.
- **Measure rather than estimate** when the answer is in the data — the search design was settled
  by pulling the real database and counting, not by reasoning about it.
- The four verification tasks under *Build and test* are the definition of done. Report test
  counts and failures as they are.

## Where to look next

`TODO.md` is the live backlog and rationale log, organised by subsystem, with "done" sections kept
deliberately because the shape of the decision is not obvious from the code. Some of its prose has
aged past the code (§4 still says no settings UI exists; one now does) — trust the code, and update
`TODO.md` when you touch a section it describes.
