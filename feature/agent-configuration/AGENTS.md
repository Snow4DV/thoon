# feature/agent-configuration

Model and provider settings, per-chat overrides, and the settings tree they hang off. Four screens:
`AgentConfigurationScreen` (root and sections), `AgentConfigListScreen`, `AgentConfigEditorScreen`,
`ChatConfigurationScreen`. The general mechanics — DI, factories, the collector, package layout — are
in `docs/architecture/FEATURES.md`; the editor presenter is the reference for `docs/circuit/PRESENTER.md`.
This file holds the decisions specific to this feature. Each is stated once, here, and not in code.

## One settings `Screen`, not one per section

`AgentConfigurationScreen(section)` covers the whole tree. The root is `section = null`: a list of
rows whose taps navigate to a section, rendered by the same code as a section. Adding a section is an
entry in `ConfigurationSection` plus a branch in `configurationSectionItems`, which an exhaustive
`when` refuses to compile without. Never a new Screen, presenter, UI or registration. Titles and
contents live in `impl`; the `api` enum is a navigation key and nothing more.

## The settings renderer

`SettingItem` is a sealed set of row types, each carrying its own lambda. `SettingsUi` renders whatever
list it is handed and knows nothing about which screen it is on. `SettingsUiState` has no event sink
because there is nothing the UI can report that an item cannot do itself. Three screens share this:
root and sections, the list, and per-chat configuration.

Grow it with new *row types*. Never with conditional fields or per-field errors — that is a form, and
the editor has its own `AgentConfigEditorUi` precisely because folding it into `SettingItem` would
have meant a form library inside a settings model. The key-and-dispatch alternative
(`SettingChanged(key, value)` and a `when (key)`) was rejected: it trades the compiler for strings in
the one place a settings screen accumulates them.

## Model & Provider is CRUD, not a settings list

The root row for it navigates straight to `AgentConfigListScreen`. The `ModelAndProvider` branch in
`configurationSectionItems` is reached only through a restored back stack; it shows a single link.

## Two sections are placeholders on purpose

Agent and Advanced render a `SettingItem.Placeholder` whose text says *why* they are empty:
`agent_settings` holds only `defaultConfigId`, so an edited system prompt has nowhere to live. Fix
storage first. Do not invent settings that cannot persist, and do not delete the placeholders — a
section that renders nothing reads as a failed load, not an unfinished feature.

## Tool approvals are toggles over rules

Tools shows the global "always allow" rules and Chat settings the chat's own, both through
`toolApprovalItems`: one `Toggle` per gated tool for its blanket rule, then one per value-bound rule,
which only switches off because such a rule is made from a call's arguments, never from settings.
The rows write through `AddToolApprovalRuleUseCase` and `RevokeToolApprovalRuleUseCase` and read
`ObserveToolApprovalRulesUseCase` scoped exactly (null is global), so a chat rule never shows under
Tools and a global one never looks chat-specific. Switching a tool *off* is still unsupported; the
Tools placeholder that remains when no tool is gated says so.

## The list opens the editor, and only the editor

Every row in `AgentConfigListScreen` opens `AgentConfigEditorScreen`; setting the default and deleting
happen there. A row that could also make-default on tap needs two tap targets, which is a richer
`SettingItem` than a list should need, and a list where a tap might do one of two things is a list
people stop trusting. The default is called out in the row subtitle because nothing else on screen
answers "which model replies when a chat has not chosen one".

## The editor

- **Fetched once.** The draft is seeded by `GetAgentConfigUseCase`, not a flow. Once the user has a
  draft, the draft is the truth; a background write must not replace what they are typing. The
  `loaded` transition is a no-op after the first load, which is what makes re-fetching after rotation safe.
- **One flat form, not a sealed one per protocol.** Switching protocol keeps the name and model id
  already typed. `AgentConfigForm.visibleFields` decides what is shown; `toDraft` drops the fields the
  chosen protocol does not have, so a leftover API key never reaches storage. A blank base URL becomes
  `null` ("use the provider default"), never `""`.
- **`ConfigTextField` takes a value and reports a value, never the form.** An earlier version took
  the form and handed back an edited copy; a Compose effect inside it closed over the form captured at
  launch, so after a protocol switch the next keystroke wrote onto the stale form and silently reset
  the protocol. Knowing only about a string makes that class of bug unrepresentable.
  `AgentConfigFormTest` pins the contract from the `write` side.
- **`normalizeBaseUrl` picks the scheme by host, not by protocol.** Private or loopback addresses
  (`10.0.2.2`, `192.168.x.x`, `localhost`, `*.local`) become `http://`, anything routable `https://`.
  Choosing by protocol would break a local OpenAI-compatible server and turn a clear validation error
  into a confusing connection failure. The completed address is stored, so reopening shows it.
- **Errors are typed all the way down.** `AgentConfigError` is a sealed type, `field()` maps each
  case to the input it belongs under, and `NotFound` maps to no field because nothing the user typed
  is wrong. Collapsing this to a string would put every failure in a banner saying "invalid".
- **On-device configurations cannot be saved while `LocalEnginesCollector` is empty.** The UI says
  so in a line of text rather than hiding the option; a silently missing option reads as a bug.
  Engines are read when the editor presenter is created, not when the factory is, so a late
  registration shows on the next open.

## Per-chat configuration is derived, never stored as a flag

`ObserveChatConfigUseCase` reports the *effective* configuration: the override if one is set,
otherwise the default. So the chat "follows the default" exactly when `effective.id == default.id`.
Comparing the two resolved values stays correct when the default changes underneath the screen; a
stored `followsDefault` flag would not. `chatConfigurationItems` selects exactly one row — none looks
broken, two lie about which model answers — and puts "Manage models" last so a chat with no suitable
model is two taps from creating one rather than a dead end.

## Presenter-test trap

`presenter.test {}` is distinct-until-changed, but the event sink is a fresh lambda every
composition, so two otherwise-identical states do not collapse. `AgentConfigEditorPresenterTest`
therefore waits for the state it describes with `awaitUntil` instead of counting emissions. Copy that
helper into any new presenter test here.
