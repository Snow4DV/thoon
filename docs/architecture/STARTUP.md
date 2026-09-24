# How the app starts

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

## What each platform installs first

Android's `rememberAppStartup` installs three context holders side by side,
`AndroidDatabaseContext`, `AndroidFilesContext` and `AndroidPreferencesContext`, before starting.
Separate holders rather than one shared one because each lives in the module that uses it
(`common:database`, `feature/agent-tools`, `common:shared-preferences:impl`), and none of those may
depend on another. All keep the application context, so the Activity handed in is not retained. They are installed
explicitly from `rememberAppStartup` rather than through an androidx.startup `Initializer`: the
ordering stays visible in the app's own startup code, and no extra `ContentProvider` runs at launch.

iOS installs nothing: its `DatabaseBuilderFactory` resolves `NSDocumentDirectory` itself.

`AppStartup.start()` is guarded by a plain flag and not synchronised. It is only ever called from
composition, which is single-threaded, and the collectors it drives are not thread-safe either.
Both phases are synchronous, so anything composed after the call may resolve holders.

## Why startup is composable

`rememberAppStartup()` is a composable because composition is the one place both platforms share:
Android enters through `setContent { App() }` and iOS through `ComposeUIViewController { App() }`,
so there is no common non-UI entry point to hang startup on. `App()` is therefore deliberately not
`@Preview`-able; it installs a database context and runs every initializer. Preview screens instead.

The `Circuit` and its saver are built inside `remember` in `App()`, which runs after
`rememberAppStartup()`, so the serializers module the saver snapshots is complete.
