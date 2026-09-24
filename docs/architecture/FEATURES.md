# How a feature is built

The mechanics shared by every feature module: the api/impl split, dependency injection, how a
screen is found and saved, the extension points, and the steps to add a feature. Decisions that are
specific to one feature live in that feature's own `AGENTS.md`; presenter rules live in
[`../circuit/PRESENTER.md`](../circuit/PRESENTER.md).

## api and impl

`feature/foo/api` holds the `Screen`s and any contract another feature needs to navigate to or talk
to this one. `feature/foo/impl` holds presenters, UI and DI. A feature depends on another feature's
`api` only, so nothing compiles against another feature's implementation.

A screen that is only ever reached from inside its own feature does not need to be in `api`; it
lives in `impl` next to its presenter (`AgentConfigListScreen`, `AgentConfigEditorScreen`). The api
module publishes the door; the rooms behind it are the feature's business.

`UiState` and event types are top-level in `impl`, never nested inside the api `Screen`. The api
module is a navigation key; nesting the state there would make every feature that merely navigates
to the screen compile against its presentation model. Circuit never needs the link: its factories
are star-projected, and only the `Screen` crosses the module boundary.

## DI — hand-rolled, no framework

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
saying why. `ApiComponentHolder.get()` caches on first call; `reset()` clears the instance but keeps the
provider.

Two holder kinds exist. `ApiComponentHolder` has its provider set by an initializer and is what
features use. `LazyComponentHolder` builds itself in `build()` and needs no initializer; the leaf
utility modules (`common:network`, `common:serialization`, `common:coroutines`) use it. Every holder's
`reset()` and `set(instance)`, and every collector's `reset()`, are test seams only: a process
registers once, at startup.

## Navigation — Circuit, resolved by collector

`App.kt` names no feature. `ScreenFactoriesCollector` maps a `Screen` type to a lazy factory:

```kotlin
ScreenFactoriesCollector.collect(ChatScreen.serializer()) { ChatComponentHolder.get().screenFactory() }
```

`CollectedScreenFactories` is one dispatcher registered as *both* `Presenter.Factory` and
`Ui.Factory`. Keying by screen type means a feature's component is not built until something
navigates to it. **Matching is by exact class** — a sealed base does not resolve, so every concrete
screen registers itself. A miss logs `No feature registered a factory for ...` rather than crashing.

**The back stack is persisted through kotlinx-serialization, on every target.** `Screen`s live in
`feature/*/api`, are `@Serializable`, and may carry value classes — `ChatScreen.chatId` is a `ChatId`.
The one `collect` call registers both the factory and the serializer: the serializer goes into a
polymorphic `SerializersModule` over `CircuitSaveable`, and `CollectedScreenFactories.circuitSaver()`
wraps it in a `SerializableCircuitSaver` that `App.kt` sets on the `Circuit`. Deriving the screen type
from the serializer is what makes it impossible to register a screen that can be opened but not saved
— since Circuit 0.38 an unregistered screen **fails the save** rather than being dropped, which would
be a crash on the first configuration change.

This is the hand-rolled counterpart of Slack's `@CircuitSerializable` + `@CircuitInject` codegen into
a DI multibinding; the collector *is* the multibinding. The reflective saver was not an option: it is
JVM-only, and iOS needs the same registration.

A module that declares a `Screen` applies `kotlinSerialization`. A saved record that no longer
restores (a renamed screen) is dropped by Circuit and logged as `Dropped a saved screen`.

## Factories

Each feature has one `Presenter.Factory` and one `Ui.Factory` (or one class implementing both).
They claim exactly the screens the feature owns and return `null` for everything else. That null is
the contract, not a fallback: Circuit asks every registered factory in turn, so a factory answering
for a foreign screen would shadow the one that owns it. Every feature has a `*FactoriesTest` that
pins this.

`ScreenFactory` bundles the presenter factory and the UI factory as one value because
`Presenter<*>` and `Ui<*>` are star-projected: handed over separately, one could be registered
without the other, and the miss would only show up on navigation.

## Extension points

A collector is written only from a `BaseInitializer.init()`; nothing else calls `collect`.

Four collectors. Three are **non-draining**; one drains. Getting that wrong is a real bug class.

| Collector | Module | Drains on read? |
| --- | --- | --- |
| `AppOnCreateActionsCollector` | `common:init` | **yes** — actions run once |
| `AgentToolsCollector` | `common:agent:tool-api` | no — the registry is rebuilt per run |
| `ScreenFactoriesCollector` | `common:navigation` | no |
| `LocalEnginesCollector` | `common:agent:api` | no |

Draining `AgentToolsCollector` would give the first run its tools and every later run none — which
looks like the model forgetting its abilities mid-session, not like a DI bug.

## Adding a feature

1. `include(":feature:foo:api")` and `":feature:foo:impl"` in `settings.gradle.kts`.
2. `api` module: the `Screen`(s), `@Serializable`, with `@Serializable` value-class ids from
   `common:agent:api` where the screen carries one. Plugins: `kotlinMultiplatform`,
   `androidMultiplatformLibrary`, `androidLint`, `kotlinSerialization`. No Compose plugins in an
   `api` module. `api(libs.circuit.runtime)`, `api(project(":common:navigation"))`, and
   `api(project(":common:agent:api"))` if an id appears in the constructor.
3. `impl` module: the six DI files, a `Presenter.Factory` and a `Ui.Factory` (both returning `null`
   for screens they do not own — that is the contract, not a fallback), and the UI. Under
   `presentation/`, **one package per screen, each split into `circuit/` (presenter, held state,
   holder) and `ui/` (composables, `UiState`, `mapper/`, `component/`)**; anything several screens
   share sits under `common/` (`common/settings/` in settings), so the screen packages
   are the only other children of `presentation/`. The factory that
   claims every screen stays at `presentation/` root. A single-screen feature skips the per-screen
   level. `feature/settings/impl` is the reference layout.
4. In `impl`'s `build.gradle.kts`: `api(project(":common:navigation"))` and
   `api(project(":feature:foo:api"))` — `api` because `ScreenFactory` and the screen type appear in
   the component's signature — then `implementation` for `:common:di`, `:common:init` and any other
   feature's **`api`** module. Never depend on another feature's `impl`.
5. In the initializer: `ScreenFactoriesCollector.collect(FooScreen.serializer()) { … }` for every
   concrete screen, then add `FooInitializer()` to `FeatureRegistry`.

`feature:agent-tools` is the exception to the api/impl split: it contributes tools rather than a
screen, so it is one module and registers into `AgentToolsCollector`.
