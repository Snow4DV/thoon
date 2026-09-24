# Storage

**One Room database for the whole app** (`ThoonDatabase` in `:shared`, currently `VERSION = 3`).

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

### Tool approval rules

`agent_tool_approval_rule` holds the "always allow" rules: `toolName`, a nullable `chatId` (null is
global), `parametersJson` (a string map; empty means the rule covers every call of the tool) and
`createdAt`. No foreign key to `agent_chat`, like checkpoints; `DeleteChatUseCase` removes a chat's
rules by hand. Parameter matching happens in Kotlin, not SQL. An unreadable `parametersJson` drops
the row rather than widening it. Decisions on individual calls are not stored at all — see the
runtime doc.

### The anchor invariant

`ChatTimelineProjector` assigns entry ids as a counter over *rendered* entries, which cannot be
inverted (system turns are skipped, a user turn collapses, an assistant turn expands per part). So
each entry also carries `messageSequence`, and **that equals `agent_chat_message.sequence`** because
`KoogMessageRowCodec.toRows` writes `sequence = index` and `toMessages` sorts by it. Search deep
links rest entirely on that equality; `ChatTimelineProjectorTest` pins it.

## Lazy everywhere

Nothing at startup opens the database. `ThoonDatabaseComponent` is built on first access, and each
DAO is registered in `DatabaseInitializer.registerDaos` as a lambda that is only invoked when that
DAO is first requested. `DaoFactoryTest` pins that registering never builds a DAO, and
`DatabaseRegistrationTest` records registrations without opening a database, which is why
`registerDaos` is `internal` and separate from `init()`.

Only DAOs cross the component boundary, never the database itself, so no consumer can open a
transaction, close the connection, or depend on the concrete database type.

## Adding a DAO

1. The `@Dao` interface is `public` and extends `ThoonDao` (a marker so `DaoFactory.get` cannot be
   called with arbitrary types).
2. Register it in `DatabaseInitializer.registerDaos`.
3. Add it to `DatabaseRegistrationTest.ALL_DAOS`. A missing registration fails at first use at
   runtime; the test turns that into a build failure.

## Migrations

`VERSION` is shared by every feature and any schema change bumps it. `ThoonMigrations.ALL` is empty
by decision: the app is pre-release and the owner reinstalls (see the working agreements). There was
no migration from version 1 either. Conversations moved from a JSON blob to message and part rows
while pre-release, and reinstalling was cheaper than migration code nobody would run again. An older
install fails loudly on open, which is exactly what the deliberate absence of
`fallbackToDestructiveMigration` is for.

## Small preferences are not in Room

A value the first frame needs — today only the theme mode — lives in `common:shared-preferences`, a
synchronous key-value store (`SharedPreferences`, `NSUserDefaults`, `localStorage`), not in
`ThoonDatabase`. Reading it through Room would open the database before anything is drawn and
render one frame in the wrong theme while the query ran.

The store knows nothing about what it holds. Each owner creates its own namespace through
`KeyValueStoreFactory.create(name)` and exposes use cases, not the store:
`common:user-settings` owns the theme mode and publishes it as a `StateFlow` seeded synchronously,
so `App()` and the settings screen read the same value with no initial `null`. An unknown stored
value reads as the default rather than failing. The store is plaintext; secrets never go there.

## Platform builders

`DatabaseBuilderFactory` is an `expect class`, not an `expect fun`: the platform halves need
different inputs (a `Context` on Android, a path on iOS), and Room's builder overloads need a
reified type parameter, which an `expect` function cannot have. Both actuals cast
`Room.databaseBuilder<RoomDatabase>(...)` to `RoomDatabase.Builder<T>`. That is sound only because
the generated initializer is always passed, so Room never takes its reflective constructor path.
Do not drop the initializer argument.

The iOS file lives in `Documents`, not `Caches`: iOS may evict `Caches` under storage pressure,
which would silently drop conversation history.

Room SQL has no host test; verify queries on the emulator (see the root AGENTS.md, Testing).
