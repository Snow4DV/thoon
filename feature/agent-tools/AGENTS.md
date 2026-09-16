# feature/agent-tools

The seven tools the agent can call. The contract, the adapter and the `web_search` triage are in
`docs/architecture/AGENT_RUNTIME.md` (Tools). This file holds the decisions specific to this module.
Each is stated once, here, and not in code.

## The file sandbox is `ChatFilePaths`, and only that

Every file tool resolves its path through `ChatFilePaths`, which normalises before the containment
check (`a/../../b`), compares segment-wise (`chat-1-notes` is not inside `chat-1`), and refuses
absolute paths and the chat folder itself. It lives in `commonMain` on okio `Path`, which is
available on wasmJs unlike `FileSystem.SYSTEM`, so a future OPFS store inherits the check unchanged.
This is the security boundary for text fetched by `fetch_url` and `web_search` (see the root
security constraints).

## `ChatFileStore` is suspending and okio-free

A web port replaces the store, not the check: okio `FileSystem` is synchronous and OPFS is not (its
sync API exists only inside a Web Worker), so an `OpfsFileSystem : FileSystem()` cannot exist.

## `edit_file`

`old_text` must match exactly once; ambiguity is refused and the excerpt around the edit is returned.
A double apply on a resumed turn is not prevented, only made visible: `new_text` containing
`old_text` passes both checks, and the excerpt is how the model notices.

## `read_file` refuses files over 256 KiB

Rather than truncating: a clipped file has the model reasoning about text it cannot see.

## `AndroidFilesContext` duplicates `AndroidDatabaseContext` on purpose

The alternative is a `common:database` → `agent-tools` dependency for one field. Both are installed
side by side in `AppStartup.android.kt`. iOS files live in `Documents`, not `Caches`, for the same
reason as the database: `Caches` can be evicted.

## `web_search`

DuckDuckGo's HTML page is the only credential-free search, and it breaks silently when the markup
moves. A `User-Agent` is required or a stub page comes back. Result links are `uddg=` redirects,
unwrapped so `fetch_url` can follow them. `DuckDuckGoParser.isChallengePage` matches the substrings
`anomaly` and `challenge` anywhere in the body, so a results page whose snippet contains the word
"challenge" would be reported as a block; known, not yet fixed. Test fixtures are captured from the
live endpoint, not invented: the redirect wrapper, `&amp;`, percent-encoding and `<b>` tags are all
real. Do not hammer the endpoint from the emulator's host.

## `fetch_url` is deliberately parser-free

`HtmlText` drops `script`, `style`, `noscript`, `svg` and `head` bodies whole, turns block closers
into newlines, decodes only the common entities, and caps output at 20,000 characters.

## `current_datetime`

Takes `now` and `zone` as inputs because `AgentClock` is `internal` to `common:agent:impl`. The
output shape is pinned exactly by its test: explicit formats, `Z` on the ISO line and `+00:00` after
"UTC".
