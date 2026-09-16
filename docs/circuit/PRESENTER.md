# How a presenter looks here

What a Circuit presenter in this repo is expected to look like, and why. Distilled from Circuit's own
docs and recipes, the `star`/`tacos` samples, Tivi (chrisbanes), and the decisions already made in
this codebase. Where this file and the code disagree, the code is the older one — fix the code.

Sources, for when a rule needs re-checking:

- Circuit docs: [Presenter](https://slackhq.github.io/circuit/docs/presenter/),
  [Scaling Presenters](https://github.com/slackhq/circuit/blob/main/docs/docs/presenter-patterns.md),
  recipes for [forms](https://github.com/slackhq/circuit/blob/main/docs/recipes/form-with-validation.md),
  [retention](https://github.com/slackhq/circuit/blob/main/docs/recipes/keep-state-across-config-change.md),
  [suspend from an event](https://github.com/slackhq/circuit/blob/main/docs/recipes/run-suspend-from-event.md),
  [loading states](https://github.com/slackhq/circuit/blob/main/docs/recipes/loading-states.md),
  [testing](https://github.com/slackhq/circuit/blob/main/docs/recipes/test-a-presenter.md).
- Samples: `samples/star` (`PetDetailScreen.kt`, `PetDetailPresenterTest.kt`), `samples/tacos`
  (`OrderTacosCircuit.kt`) in the Circuit repo.
- Tivi: `ui/episode/track/.../EpisodeTrackPresenter.kt`, `ui/settings/.../SettingsPresenter.kt`.

## The shape

```kotlin
internal class FooPresenter(
    private val screen: FooScreen,               // assisted: comes from the factory
    private val navigator: Navigator,            // assisted: comes from the factory
    private val observeFoo: ObserveFooUseCase,   // everything else is a constructor dependency
    private val saveFoo: SaveFooUseCase,
) : Presenter<FooUiState> {

    @Composable
    override fun present(): FooUiState {
        val scope = rememberCoroutineScope()

        // (1) observed truth, collected, never copied
        val foo by remember(screen.id) { observeFoo(screen.id) }.collectAsState(initial = null)

        // (2) held state: one retained holder
        val editor = rememberRetained { FooStateHolder() }

        // (3) map held + observed to the UI state
        val current = foo ?: return FooUiState.Loading(onBack = navigator::pop)
        return when (val held = editor.value) {
            is FooState.Editing -> FooUiState.Data(
                title = current.title,
                draft = held.draft,
                canSave = editor.canSave(),
                onBack = navigator::pop,
                eventSink = { handleEvent(it, editor, scope) },
            )
        }
    }

    // (4) handling, outside the composable
    private fun handleEvent(event: FooUiEvent, editor: FooStateHolder, scope: CoroutineScope) {
        when (event) {
            is FooUiEvent.Ui.DraftChanged -> editor.edit(event.value)
            FooUiEvent.Ui.SaveClicked -> editor.saveableDraft()?.let { draft ->
                scope.launch {
                    editor.busy()
                    saveFoo(draft)
                    navigator.pop()
                }
            }
        }
    }
}
```

The numbered comments are for this page; the real presenter has none of them. Read top to bottom:
observe, hold, return, then handle. `present()` is the composable part and stays
short; a presenter whose `present()` cannot be read in that order is asking to be split (see *When
it grows*).

## Rules

### Presenters hold only presentational state

A draft, an expansion set, a selected tab, an in-flight flag. Everything that is *true about the
world* — the chat, the config, the default — is observed from a use case and never copied into a
local `var`. The one exception is a form being edited: the saved value is **fetched once** to seed
the draft, through a one-shot use case (`GetAgentConfigUseCase`), not a flow. A background write
replacing what the user is halfway through typing would be worse than showing a stale value, and a
flow dependency would invite exactly that. The seed lands through a transition (`loaded`) that is a
no-op once a draft exists, so the effect can safely run again after rotation.

`ChatPresenter` is the canonical example (conversation content comes from storage; only the draft
prompt and expansion set are held). `ChatConfigurationPresenter` shows the corollary: derive
`followsDefault` from two observed values rather than holding a flag that has to be kept in sync.

### One value, not a handful of remembers

What the presenter holds is **one retained value of its own sealed type**, in its own file, and a
small `@Stable` **holder** that owns that value and is the only thing allowed to change it:

```kotlin
internal sealed interface FooState {
    data object Loading : FooState
    data class Missing(val id: FooId) : FooState
    data class Editing(val draft: String, val error: String? = null, val isBusy: Boolean = false) : FooState
}

@Stable
internal class FooStateHolder(id: FooId?) {
    var value: FooState by mutableStateOf(if (id == null) FooState.Editing("") else FooState.Loading)
        private set

    fun edit(value: String) {
        val current = this.value as? FooState.Editing ?: return   // nothing to edit yet: no-op
        this.value = current.copy(draft = value, error = null)     // an edit clears its rejection
    }
    fun busy() { value = (value as? FooState.Editing)?.copy(isBusy = true) ?: return }
    // one method per transition; no setter
}
```

The holder is what Circuit's form recipe means by a "presentation state holder": created once with
`rememberRetained`, owning the Compose state, exposing named operations. The sealed type carries no
functions of its own — not even extension functions — so there is exactly one place to read to learn
what can happen to the state, and the presenter's event branches read as `editor.edit { ... }` and
`editor.busy()` rather than as reassignments of a composable-local variable.

Every transition is a no-op on a variant it does not apply to, so the presenter never checks the
variant before calling one. `mutableStateOf` works outside a composition, so the holder is tested
with plain `kotlin.test` and no Compose runtime. `AgentConfigEditorState` and
`AgentConfigEditorStateHolder` are the reference.

Three reasons, in order of weight:

1. **Transitions are atomic.** An edit and the clearing of its error, or a save settling and its
   rejection landing, are one snapshot write. With separate `remember`s they are two, and under
   Circuit's `Immediate` recomposition (which `presenter.test {}` uses) the frame in between is
   observable — a test caught exactly this. A frame showing new text under an old rejection is not
   a state the screen can be in, so the model should not be able to express it.
2. **Transitions are testable without a composition.** `AgentConfigEditorStateHolderTest` covers
   the rules in plain `kotlin.test`; the presenter test then only has to cover wiring.
3. **The variants are explicit.** `Loading | Missing | Editing` in the held state maps one-to-one
   onto `Loading | Error | Data` in the UI state, and a flag like `isMissing` that only means
   something when another value is null disappears.

Separate from the UI state on purpose: the UI state also carries what is *observed* (a default, a
registry) and the event sink; the held state is only what the presenter *owns*. Observed values stay
as collected flows beside it — they are not copied in. `AgentConfigEditorState` is the reference.

Precondition checks are holder methods too (`idleForm()`, `saveableForm(engines)`), so an event
branch reads as "if there is a saveable form, launch" rather than as a chain of casts and flags.

### Retain, do not remember

Presentational state is `rememberRetained`, not `remember`. Circuit's retention recipe is explicit:
`remember` dies on rotation; `rememberRetained` survives rotation and the back stack with no
`Parcelable`. This is what makes a half-typed form survive turning the phone. `ChatPresenter` already
retains `createdChatId` for this reason — a second conversation being created on rotation was the
bug that taught it.

Use plain `remember` only for something you would happily recompute: a `Flow` built from a stable
key, a `CoroutineScope`, a derived list.

Do **not** retain a `Navigator`, a `Flow` or anything lifecycle-bound; retain values.

### Observed flows: `remember(key) { flow }.collectAsState()`

Build the flow inside `remember` keyed on its inputs, then collect. Building it inline re-subscribes
on every recomposition. `produceState`/`produceRetainedState` is the alternative when the collection
needs to do more than assign (`ChatPresenter` uses it with `collectLatest`).

### Sealed state: `Loading | Error | Data`

State is a sealed interface with only the events that make sense on each variant. `Loading` and
`Error` carry nothing the UI can act on except what it needs to draw a frame — a title, and an
`onBack: () -> Unit` in the style of `SettingsUiState`. The event sink lives on `Data` alone, so the
type says that only a loaded form can be edited or saved; a `Loading` that accepts `SaveClicked` and
drops it is a lie the compiler could have caught. A screen that has data to load must not render its
`Data` shape with blanks in it while the load is in flight — that is how an editor shows an empty
form under an "Edit" title for a frame, and how a keystroke made during that frame gets overwritten.

### No flag that depends on another flag

Circuit's *Scaling Presenters* guide calls this boolean flag soup. Two forms of it, and the fix for
each:

- **A value that only means something when another is set** is a nested nullable sub-state, not a
  pair of flags. `isDefault` means nothing while `isEditing` is false, so `Data` carries
  `existing: Existing?` with `Existing(isDefault)`, null while creating. The UI reads
  `state.existing?.let { ... }` and the impossible combination cannot be built.
- **Two nullables that are never both set** are one sealed nullable. A refusal is about one input or
  about none, never both, so the editor carries `rejection: Rejection?` with `Field(field, message)`
  and `Screen(message)` rather than `fieldError` beside `screenError`.

The same rule produced `Loading | Missing | Editing` in the held state: `isMissing` was a flag that
only meant something while the draft was null.

A screen with *nothing* to load may use a single data class; the settings renderer's
`SettingsUiState` is one.

### Event handling is a plain function, not a lambda in `present()`

The sink is `{ handleEvent(it, editor, scope) }` and `handleEvent` is a private, non-composable
function that takes the holder and the coroutine scope as parameters. This is Circuit's *Scaling
Presenters* Pattern 1 applied from the start rather than after the fact, for two reasons:

- `present()` then contains only what is composable — observing, holding, mapping — and reads in
  one screen. The handling, usually the longest part, is read on its own without Compose in the way.
- Passing the holder and scope in, rather than closing over composable locals, keeps the function
  free of composition state. It can be called, and reasoned about, like any other method.

Do not extract further than this at the editor's size. One `handleEvent` with one exhaustive `when`
is the shape; a `when` nested inside a branch is the signal to look at the event hierarchy instead.

### Events are intents, not payloads

An event names what the user did — `NameChanged(value)`, `KindSelected(kind)`, `SaveClicked` — and
the presenter decides what that means. The UI never computes the next state and posts it back
whole. Circuit's form recipe, Tivi's `EpisodeTrackPresenter` and this repo's `ChatPresenter` all
work this way. The cost is more event classes; the gain is a UI with no business knowledge in it and
an event log a person can read.

Events nest under `Ui` (`FooUiEvent.Ui.SaveClicked`) so that a second source — a dialog result, an
overlay — has a sibling namespace to live in.

### Save is guarded

A button that launches a suspend call has an in-flight flag, and the flag feeds `canSave`. Without
it two taps create two rows. Circuit's form recipe and Tivi both carry `isSubmitting`; the tacos
sample gates `Next` on validation the same way.

### `rememberCoroutineScope` is for work that may be abandoned

Circuit's recipe: the scope dies with the composition, so use it for a save or a toggle whose result
stops mattering once the user leaves. Anything that must complete regardless of the screen goes to a
process-scoped owner — here that is the run coordinator (`SendPromptUseCase` enqueues; the presenter
does not wait for the answer). If the result still matters after the user leaves, it does not belong
on this scope.

### Errors land where they belong

A typed error goes under the field that caused it; a failure about no one input is a banner. Both
are one `Rejection?` (see above). Any edit clears it — leaving a rejection under a field the user
has since corrected is how a form starts lying. Never collapse a typed error to `"invalid"`.

### Everything comes through the constructor

Screen and navigator are assisted (passed by the factory); every other collaborator is a constructor
parameter, including registries. A presenter that reaches into a global collector cannot be tested
without that collector being populated, and it is the only object in the graph resolving its own
dependencies.

### The factory claims exactly its screens

`Presenter.Factory.create` returns `null` for screens it does not own. That is the contract, not a
fallback; a factory that answers for a foreign screen shadows the one that owns it. Every feature
has a `*FactoriesTest` pinning this.

### `present()` emits no UI

`Presenter.present` is `@ComposableTarget("presenter")`. No composables from `compose.ui` or
`compose.foundation` are called in a presenter, only `compose.runtime`.

## Where the files go

One package per screen under `presentation/`, and inside it two packages that must not depend on
each other the wrong way round:

```
presentation/
  FooFactories.kt            claims every screen the feature owns
  common/                    anything two screens share, in named sub-packages (common/settings/)
  editor/                    every other child of presentation/ is a screen
    EditorScreen.kt          the navigation key, if it is internal to the feature
    circuit/                 EditorPresenter, EditorState (held), EditorStateHolder
    ui/                      EditorUi, EditorUiState (+ events, sub-states), mapper/, component/
```

`circuit/` depends on `ui/` — the presenter builds the `UiState` and reads the event types. `ui/`
never depends on `circuit/`: a composable knows its state and its events and nothing about how they
are produced. Mappers between domain and form live under `ui/mapper/` because they define the shape
the UI renders, and the presenter is merely their caller. A pure function that builds part of a
`UiState` (a list of rows) sits in `circuit/` beside the presenter that calls it, since it is
presentation logic, not drawing.

Tests mirror the packages exactly. `feature/agent-configuration/impl` is the reference.

## When it grows

Circuit's *Scaling Presenters* guide, in order of reach:

1. **Decompose** — pull observation into private `@Composable observeX()` functions. Event handling
   is already out of `present()` (see *Event handling is a plain function*). Do this when
   `present()` still does not read top to bottom.
2. **State holders** — a `@Stable` class owning a field's value, error and validation, created with
   `rememberRetained`. Do this when a form has several fields that each validate.
3. **StateProducer** — a class with a `@Composable produce(): X`, injected into presenters, never a
   screen of its own. Do this when two screens observe the same thing the same way.
4. **Composite presenters** — a presenter that calls child presenters' `present()`. Rare; only for a
   dashboard whose parts could each be a screen.

Do not reach for any of these at a hundred lines. A presenter that observes two flows, holds a
draft and handles five events is the normal size and should stay in one function.

## Testing

Presenters are tested with `circuit-test`: `presenter.test { awaitItem() }` drives `present()` in a
`runTest`, `FakeNavigator(screen)` records navigation (`awaitNextScreen()`, `awaitPop()`).

- Consume `Loading` first; a presenter that starts loading emits it before `Data`.
- `awaitItem()` is distinct-until-changed: assert on state *changes*.
- Drive the presenter through `eventSink` on an emitted state, never by calling private members.
- Fakes for use cases live in `commonTest/.../fake/`, as elsewhere.

Test names are sentences (`saveIsIgnoredWhileASaveIsInFlight`) and assertion messages state the
consequence, not the mechanics — the same convention as the rest of the repo.

## Checklist

Before calling a presenter done:

- [ ] Presentational state is one sealed type of plain data in its own file, changed only through
      a `rememberRetained` `@Stable` holder with one method per transition; observed truth is
      collected beside it, not copied in.
- [ ] State is sealed; `Data` never renders with placeholders for something still loading; the
      event sink is on `Data` only and other variants carry `onBack`.
- [ ] No flag whose meaning depends on another flag, and no two nullables that are never both set:
      nest the first (`existing: Existing?`), seal the second (`rejection: Rejection?`).
- [ ] Events are intents; the UI computes nothing it sends back.
- [ ] Event handling is a private non-composable `handleEvent(event, holder, scope)`; the sink in
      `present()` only points at it.
- [ ] Every launched action that writes has an in-flight guard feeding an `enabled` flag.
- [ ] Errors are typed and land under their field, or in the banner, as one `Rejection?`; editing
      clears it.
- [ ] No global lookups inside `present()`; every collaborator is a constructor parameter.
- [ ] The factory declines foreign screens and a test pins it.
- [ ] A `presenter.test {}` covers the load, the happy path and the rejection path.
- [ ] No KDoc restating this document or the feature's `AGENTS.md`; a comment survives only for a
      local, non-obvious reason (see the comment rule in the root `AGENTS.md`).
