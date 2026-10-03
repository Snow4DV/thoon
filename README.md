# Thoon KMP

An AI-agent chat app for Android, iOS and the web (WASM) targets, written with the help of Kotlin 
Multiplatform and Compose Multiplatform. You chat with a model, and the model can call tools: 
it can read and write files in a sandbox for each chat, fetch web pages, search the web and
check the time. 

You can run multiple agents asynchronously: give multiple tasks to your agents and wait for the
result.

## KMP & CMP
KMP and CMP are used for this project so both business logic and UI are reused between platforms.
There are some limitations though:
* Web requires server to use HTTPS to use OPFS for database storage
* CORS doesn't allow to fetch/web_search in web version of the application now (proxy is needed to bypass)
* No WorkManager/BGTaskScheduler are used yet for ios/android to allow background work

## Architecture
Multimodule architecture was used for the project:
* common layer with api/impl separation for logic reusable between features
* feature layer with api/impl separation

To allow feature modules being wired as loosely as possible, manual alternative for java spi was
written - now initializers for feature modules are wired manually and initialized once when app
is launched.

Navigation & presentation architecture - for that Circuit framework is used that does two things:
* Easy navigation between screens in apps with multimodule architecture - api module provides
Screen class for navigation and impl provides factory that resolves this circuit's screen class
to a real Compose screen
* Neo MVP on presentation layer with compose compiler taking care of reactive state. Much less
boiler plate than MVI/Tea and etc

## AI chat driver - Koog
Koog by JetBrains is driving the agent, tool calls and etc. A lot of feature to extend later out
of the box & works perfectly for agentic tasks & tool execution

## Features
* Works with OpenAI-compatible APIs (OpenAI, OpenRouter, LM Studio & etc), Anthropic and Ollama.
Ollama uses its native /api/chat because the OpenAI-compatible endpoint drops reasoning
* You can save multiple model configs, pick a default one and override it per chat
* 7 tools for now: list_files, read_file, write_file, edit_file, fetch_url, web_search
(DuckDuckGo, no API key needed) and current_datetime
* Tool calls need approval (except current_datetime). You can allow a call once, always for this
chat or always everywhere
* Chats are saved to the database step by step, so if the app gets killed mid-answer it picks up
where it stopped on next launch
* Search through chats, light & dark theme

On-device models are planned but not implemented yet.

## Tech stack
* Kotlin 2.4, Compose Multiplatform 1.12
* Koog for the agent
* Circuit for navigation and presenters, DI is written by hand
* Room 3 for the database (SQLite-WASM + OPFS on web)
* Ktor for network, okio for files (OPFS on web)
* composables.com UI + Lucide icons

## Running
You need Android Studio, JDK 17+ and Xcode if you want to run iOS.

### Android
Run the androidApp configuration or:
```bash
./gradlew :androidApp:assembleDebug
```

Easiest way to try it without an API key is Ollama running on your computer. Add an Ollama config
in Settings → Model & Provider with `10.0.2.2:11434` as the base URL (that's your computer as seen
from the emulator). Local addresses get `http://` automatically, everything else gets `https://`.

### iOS
Open `iosApp/` in Xcode and run. If iOS tests or framework linking fail, Xcode is probably not
selected:
```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

### Web
```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

The dev server proxies Ollama at `<page origin>/ollama` so there are no CORS problems. Use that URL
as the base URL in the config. If Ollama is not on `localhost:11434`, set `THOON_OLLAMA`.

To open it from a phone in the same network you need HTTPS, otherwise OPFS won't work:
```bash
THOON_HTTPS=1 ./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

## Build and test
Before calling a change done I run all of these. iOS compile is important here - some Koog APIs
are JVM-only and compile fine on Android but break iOS.
```bash
./gradlew :androidApp:assembleDebug
./gradlew testAndroidHostTest
./gradlew compileKotlinIosSimulatorArm64 compileKotlinIosArm64
./gradlew :webApp:wasmJsBrowserDistribution
```

Tests are in commonTest. The model is faked with StubLLMClient, so the whole agent with tool calls
is tested without network. Room queries and Compose UI are not covered by tests yet, I check them
by hand.

## Project structure
```
androidApp/ iosApp/ webApp/        entry points for each platform
shared/                            App.kt, database, feature registry, startup
common/agent/                      agent api, Koog runtime & storage, tool contract
common/...                         navigation, database, di, ui, markdown, prefs & etc
feature/chat                       chat screen
feature/chats-list                 chats list and search
feature/settings                   model configs, per chat settings, theme
feature/agent-tools                file, web and date/time tools
```

## Docs
* [AGENTS.md](AGENTS.md) - start here, conventions and known traps
* [AGENT_RUNTIME.md](docs/architecture/AGENT_RUNTIME.md) - how a prompt turns into an answer,
tool approval, checkpoints
* [STORAGE.md](docs/architecture/STORAGE.md) - database and conversation schema
* [FEATURES.md](docs/architecture/FEATURES.md) - DI, navigation, how to add a feature
* [STARTUP.md](docs/architecture/STARTUP.md) - what happens on launch on each platform
* [PRESENTER.md](docs/circuit/PRESENTER.md) - how presenters are written
* [TODO.md](TODO.md) - backlog and why things are the way they are
