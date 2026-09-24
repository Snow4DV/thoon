# common/database

The Room builder and the DAO factory. How storage fits together is in
`docs/architecture/STORAGE.md`. This file holds what is specific to this module.

## The unchecked cast in every `DatabaseBuilderFactory` actual is load-bearing

Room's builder is `inline reified`; an `expect` member cannot be. Every actual therefore calls
`Room.databaseBuilder<RoomDatabase>(...)` and cast to `RoomDatabase.Builder<T>`. The reified type is
only used to reflectively locate a constructor when no factory is given, and the initializer is
always supplied, so `build()` returns exactly what the initializer produced. Do not "simplify" by
dropping the initializer argument; that is the path that makes the cast unsound.

## The Android `Context` is installed explicitly

`AndroidDatabaseContext` is set from `rememberAppStartup`, not through an androidx.startup
`Initializer`. Ordering stays visible in the app's own startup code and no extra `ContentProvider`
runs at process launch. There is no `Application` subclass, so nothing is installed from
`Application.onCreate`.

## The iOS database dispatcher is `Dispatchers.Default`

Not because `IO` is unavailable on Native: `kotlinx.coroutines.IO` exists as an extension and
`common:coroutines` already uses it in common code. `Default` is enough here because Room serialises
its own writes and the bundled driver locks; queries only need to stay off the main thread.

## On web the driver is a Web Worker, and the worker is ours

Room 3 is the first Room with a web target, and its `WebWorkerSQLiteDriver` only speaks a message
protocol; androidx ships no worker. `worker/` is a local npm module implementing that protocol over
SQLite-WASM, persisting through OPFS (`OpfsDb`). OPFS needs `SharedArrayBuffer`, so the page must be
cross-origin isolated: `webApp/webpack.config.d` sets COOP/COEP for the dev server, and any real host
must send the same headers or opening the database fails.

The worker is adapted from androidx's `room-web-demo` with two changes: it no longer logs every
request (SQL bindings carry conversations and API keys), and it closes statement and database id 0,
which the demo's truthiness check skipped.
