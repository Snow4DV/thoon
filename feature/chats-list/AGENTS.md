# feature/chats-list

The list of conversations, its search, and the bottom bar. General mechanics are in
`docs/architecture/FEATURES.md`; presenter rules in `docs/circuit/PRESENTER.md`. This file holds the
decisions specific to this screen. Each is stated once, here, and not in code.

## Creation happens in the chat screen, not here

"New chat" navigates to `ChatScreen(chatId = null)` and the chat screen creates the row. The list never
calls `createChat`, so there is exactly one creator and it is the screen that owns the id. See
`feature/chat/AGENTS.md` for the consequence (an abandoned new chat is an untitled row here).

## One flow for both tabs

Both tabs read `searchChats(query)`; a blank query lists everything. The Chats tab is the Search tab
with an empty query, so the screen never swaps between two flows and cannot disagree with itself
mid-transition. Switching back to Chats clears the query: a list silently filtered by something typed
minutes ago reads as chats having gone missing.

## Two kinds of empty

`Emptiness.NoChats` and `Emptiness.NoMatches` are distinct. Only the first offers "Create a new chat";
offering it as the answer to a search that found nothing answers a question nobody asked.

## Rows

- A chat is titled from its first prompt, so one abandoned before asking has no title. The label
  falls back to `Chat <first 8 characters of the id>`, and that fallback is decided in the mapper,
  not the UI.
- The subtitle prefers the matched snippet over the last message. A result that cannot show why it
  matched is a filter, not a search. Snippets arrive already windowed around the match and are not
  re-truncated; re-truncating is what used to cut the match off.
- One chat can match several times, so `key` is `"$id#$messageSequence"` while `id` stays the chat id,
  which is what opening and deleting act on.
- Every case-insensitive occurrence of the query in the label and subtitle is styled, because a
  windowed snippet puts the match on the line but nothing says which words were asked for. The
  styling is a plain function taking a `SpanStyle`, so it stays composition-free and testable. A blank
  query styles nothing; it would otherwise match at every index and never advance.

## The bottom bar

A floating pill built by hand: the library `NavigationBar` is edge-to-edge with a separator above it.
New chat is an action button, not a tab. A tab is a place you can be, and a selected "new chat" tab
would point at a screen nobody is on. Tint travels through `LocalContentColor` because
`NavigationBarItem` styles only its background on selection. The bar rides above the keyboard so the
way out of Search is never hidden behind it, and the list keeps bottom padding so the bar does not sit
on the last row. The search field focuses itself when its tab is selected; choosing the tab is the
whole intent.

## The screen takes the status inset itself

There is no top bar to paint behind the status bar, and the Circuit host applies no insets, so this
screen consumes them directly.
