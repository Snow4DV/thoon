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
./gradlew testAndroidHostTest                # every module's host tests (254 today)
./gradlew compileKotlinIosSimulatorArm64 compileKotlinIosArm64   # iOS must keep compiling
./gradlew :webApp:wasmJsBrowserDistribution  # web (wasmJs) must keep building
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
- **A presenter test that fails with `Method e in android.util.Log not mocked` is hiding the real
  error.** Compose's Recomposer logs a composition failure through `android.util.Log` before
  rethrowing it, and on the host that call is a stub that throws first. The module's
  `withHostTestBuilder {}.configure { isReturnDefaultValues = true }` is what lets the real
  exception through; copy it into any module that gains presenter tests.
- Before adding a dependency, look in `~/.gradle/caches/modules-2/files-2.1/` — several libraries
  (kotlinx-datetime among them, until it was made explicit) are already resolved transitively for
  all three targets, which is what makes adding them low-risk.

**Git history is not a guide here.** Two commits exist and most of the tree is uncommitted, so
`git log` tells you nothing about why code looks the way it does. The docs and the nearest
`AGENTS.md` do — see Conventions.

## Module map

```
androidApp/            Android entry point (MainActivity)
iosApp/                Xcode project
webApp/                wasmJs entry point (ComposeViewport)
shared/                composition root: App.kt, ThoonDatabase, FeatureRegistry, startup
common/
  agent/api            use cases + models the app talks to (no Koog types)
  agent/impl           agent runtime, storage, Koog integration
  agent/tool-api       tool contract, Koog-free, so features can add tools
  navigation           Screen factory collector + the back-stack saver
  database             DaoFactory, database builder (expect/actual)
  shared-preferences/{api,impl}   synchronous key-value store (SharedPreferences / NSUserDefaults / localStorage)
  user-settings/{api,impl}        app-level preferences that are not the agent's — the theme mode
  di  init             ComponentHolder / BaseInitializer plumbing
  ui  markdown  log  coroutines  network  serialization
feature/
  chat/{api,impl}                 the conversation screen
  chats-list/{api,impl}           list, search, bottom bar
  settings/{api,impl}             the settings tree: model & provider, per-chat overrides, appearance — see its AGENTS.md
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

### DI, navigation, extension points, adding a feature

All in [`docs/architecture/FEATURES.md`](docs/architecture/FEATURES.md). The short version: DI is
hand-rolled (six files per feature, holders resolved per access, every DI type `internal` except the
initializer); screens are found and saved through `ScreenFactoriesCollector`, one `collect` per
screen carrying its serializer; four collectors exist and only `AppOnCreateActionsCollector` drains.
**A new feature is invisible until its initializer is added to `FeatureRegistry`.**

### Startup order, storage

Both in `docs/architecture/`: [`STARTUP.md`](docs/architecture/STARTUP.md) (no `Application`
subclass, two ordered phases, what each platform installs first) and
[`STORAGE.md`](docs/architecture/STORAGE.md) (one Room database in `:shared`, DAOs travel back up
through `DaoFactory`, the conversation schema, the anchor invariant, no migrations). Two lines worth
repeating here: **a new feature is invisible until its initializer is added to `FeatureRegistry`**,
and **a DAO added without registering it in `DatabaseInitializer` fails at runtime**.

## The agent runtime

In [`docs/architecture/AGENT_RUNTIME.md`](docs/architecture/AGENT_RUNTIME.md): a prompt's life from
durable row to committed conversation, what a chat is "waiting on", checkpoints, and the seven tools.
Module decisions and Koog 1.1.1 quirks are in `common/agent/AGENTS.md`; the tools' own decisions in
`feature/agent-tools/AGENTS.md`. Three lines worth repeating here: **`ChatMemory` replaces the prompt
with restored history, so the system message must be restored too and editing it reaches new chats
only**; **Koog's reflective tool API and `openAIClient()` are JVM-only and break the iOS build**; and
**the `web_search` anti-bot interstitial is served as HTTP 202**.

## Testing

What exists to test each layer, and what does not:

| Layer | Use | Where |
| --- | --- | --- |
| Agent graph end to end | `StubLLMClient` — scripts the model's replies, tool calls included | `common/agent/impl/.../commonTest/fake/` |
| Anything needing the DI graph | `TestAgentModule` — the real module over in-memory fakes | same |
| HTTP tools | Ktor `MockEngine` (`ktor-client-mock` is a test dep of `agent-tools`) | `WebSearchToolTest` |
| File tools | okio `FakeFileSystem` | `OkioChatFileStoreTest` |
| Pure logic (codecs, projector, snippets, highlighting) | plain `kotlin.test` | throughout |
| Presenters | held-state transitions in plain `kotlin.test`; wiring via `circuit-test` (`presenter.test {}` + `FakeNavigator`) | `AgentConfigEditorStateHolderTest`, `AgentConfigEditorPresenterTest` |
| **Room SQL** | **nothing** — no test instantiates `ThoonDatabase` | verify by hand on the emulator |
| **Compose UI behaviour** (scroll, highlight, follow-the-bottom) | **nothing** | verify by hand |

Room does validate every `@Query` at compile time through KSP, so a projection that does not match
its row class fails the build — that catches shape, not semantics. Neither in-memory fake
reimplements Room's logic: `InMemoryChatHistoryRepository` stores whole conversations with no diff,
and `InMemoryChatMetadataRepository` matches titles only and returns no snippet.

In `runTest` harnesses, launch the agent scope and collectors as `CoroutineScope(coroutineContext +
Job())` children of the test scope, not `backgroundScope`: `advanceUntilIdle()` does not advance
`backgroundScope` coroutines. The back-stack saver is tested for registration only
(`ScreenSaverRoundTripTest`); its Bundle layer runs only on a device.

## Conventions

**Comments are the last resort, not the record.** The record is `docs/architecture/` for how the
app is built, `docs/circuit/PRESENTER.md` for presenters, and the nearest `AGENTS.md` for decisions
and traps. Code gets a comment only when all three hold: the reason is local to that line or type;
it is not obvious to an experienced KMP/Compose developer; and it is not already written in a doc.
When it is in a doc, do not link to it from code — the doc is where a reader looks first. In detail:

- No KDoc whose first sentence the name already says.
- No comment explaining a framework concept (what a factory is, why a `when` is exhaustive).
- A reason is written once. If two files want the same reason, it belongs in a doc.
- One to two lines. A paragraph is a doc that has not been written yet.
- Tests: the name is the sentence and the assertion message states the consequence. A one-line
  comment is allowed only where an assertion would otherwise look tautological.
- Companion constants (`TAG`, `TITLE`) are never commented.
- The one exception: a public `api` module keeps a one-line contract on each use case or model
  property that a caller cannot infer from the signature ("returns once durable, not when
  answered"; "null follows the default"). A caller in another module reads the interface, not a
  nested `AGENTS.md`. Rationale still goes to the docs.

`feature/settings` is the reference for what this looks like applied; every module now
follows it, and the ones with decisions of their own have a nested `AGENTS.md`.

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
  everything else from storage. **The full shape, rules and checklist are in
  [`docs/circuit/PRESENTER.md`](docs/circuit/PRESENTER.md)** — read it before writing or reviewing a
  presenter; it is the reference the code is held to.
- UI comes from `com.composables.ui` (a third-party design system) plus `common:ui`. Message bodies
  render as markdown via `ThoonMarkdown`. **Feature-specific UI does not go in `common:ui`.**

## Traps

- **Delete-and-reinstall** after a schema change: there are no migrations by design.
- **A comment that repeats a doc is a bug.** Delete it, then check the doc still says it.
- `com.composables.ui.components.Text` has an `AnnotatedString` overload, but `overflow` defaults to
  `TextOverflow.Clip`, not `Ellipsis`.
- The chat list is `reverseLayout = true` over `items.asReversed()` — visual index is
  `items.lastIndex - chronologicalIndex`, and index 0 is the *newest*.
- **Screens are transparent; `App()` paints the theme background.** Android's window background
  used to stand in for it; web has none. In dark mode `panelColor` is `#171717` on `#0A0A0A`, so a
  an attachment chip uses `Modifier.panel(shape)` from `common:ui` for its outline. The user message
  bubble and the chat-list tab pill are fill-only by choice — the outline looked wrong there.
- `snapshotFlow` emits its current value immediately. The chat screen's follow-the-bottom logic
  depends on this; anything trying to pre-set a flag it writes will be overwritten in the first frame.
- Ktor logging is capped at `LogLevel.INFO` deliberately: headers carry API keys and bodies carry
  conversations.

## Security constraints

Currently accepted, and worth not making worse:

- **API keys are stored in plaintext** in `agent_config.payloadJson`. App-private storage is the only
  protection; treat a device backup or a rooted device as key exposure. A `SecretStore`
  (Keystore/Keychain) is the planned fix. `AgentConfig.Ollama` holds no credential.
- `common:shared-preferences` is plaintext too (`shared_prefs/`, `NSUserDefaults`, `localStorage`).
  It holds small UI preferences only; a key or a token never goes there.
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
- **The feature pattern is fixed: direction in `api`, screen factory in `impl`, registered
  through a collector from the initializer — lazily, keyed by screen.** The naming is chosen
  (`ScreenFactory`, `factory()`, `obtain()`). Match it; do not propose a routing table or a base
  class in its place — both were considered and turned down.
- **Factories and components are built lazily**, on first navigation, never at startup. "A lot of
  factories just to open a screen" is the thing this exists to prevent.
- **The four verification tasks under *Build and test* are the definition of done — iOS compile
  included, every time.** The owner develops on the Android emulator; iOS is still a shipping
  target, and the iOS compile is the only thing that catches JVM-only Koog APIs. Report test counts
  and failures as they are.

## Where to look next

`TODO.md` is the live backlog and rationale log, organised by subsystem, with "done" sections kept
deliberately because the shape of the decision is not obvious from the code. Some of its prose has
aged past the code (§4 still says no settings UI exists; one now does) — trust the code, and update
`TODO.md` when you touch a section it describes.

`docs/architecture/` explains how the app is built; `docs/circuit/PRESENTER.md` how a presenter is
written. A feature with decisions of its own has an `AGENTS.md` in its directory — read it before
touching that feature.
