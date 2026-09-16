# common/database

The Room builder and the DAO factory. How storage fits together is in
`docs/architecture/STORAGE.md`. This file holds what is specific to this module.

## The unchecked cast in both `DatabaseBuilderFactory` actuals is load-bearing

Room's builder is `inline reified`; an `expect` member cannot be. Both actuals therefore call
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
