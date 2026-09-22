# feature/chat

The conversation screen. General mechanics are in `docs/architecture/FEATURES.md`; presenter rules in
`docs/circuit/PRESENTER.md`, which names `ChatPresenter` as the example of a presenter that holds
only the draft and reads everything else from storage. This file holds the decisions specific to the
chat screen. Each is stated once, here, and not in code.

## Creation happens here, on first composition

`ChatScreen(chatId = null)` means "a new conversation". `ChatPresenter` calls `createChat` in a
`LaunchedEffect` on first composition and keeps the id in a retained `createdChatId`, so a rotation
or pop-and-return does not mint a second chat. The consequence: a new chat backed out of before the
first prompt still exists, as an untitled empty row in the list. The list is never the creator; there
is exactly one, and it is the screen that owns the id.

## Follow the bottom

The list is `reverseLayout = true` over `items.asReversed()`, so index 0 is the newest item (see the
root AGENTS.md trap for the index math). Four things about following are not obvious:

- `isFollowing` is sampled when a scroll *settles* (`isScrollInProgress` flips false), never when an
  item arrives. An append moves the anchored key from index 0 to 1, so a read made afterwards says
  "not at the bottom" exactly when the reader was. A programmatic `scrollToItem(0)` settles at 0 and
  correctly leaves it true.
- A streaming message needs no help: under `reverseLayout` it grows upward and stays on screen. Only
  a *new* item needs `scrollToItem(0)`, because the scroll position follows its anchor key and leaves
  an appended item just off the bottom. The scroll is instant, not animated, because appends arrive
  mid-stream and animations would queue. The effect is keyed on the list's size, since the list's
  identity changes every streaming frame.
- A search deep link owes a jump before following may start. `isJumpPending` is a separate flag
  because `isFollowing` cannot be pre-set to false: `snapshotFlow` emits its current value immediately
  and would overwrite it in the first frame. The flag is cleared even when the target is not found,
  or following would stay off for the rest of the visit.
- `spacedBy` keeps its default `Alignment.Top` on purpose: `reverseLayout` inverts the arranged
  offsets, so `Top` is what rests a short chat on the prompt field.

## Search deep link

`ChatScreen.highlightMessageSequence` is the search hit's `agent_chat_message.sequence` (the anchor
invariant in the root AGENTS.md). `resolveHighlightedItemId` turns it into an item id: one stored turn
projects as several items sharing that sequence, reasoning first, so a user or assistant message
wins and any other carrier is the fallback so the hit is still shown. Null when nothing carries the
sequence (history compression, a checkpoint whose indices moved) opens the screen normally.

`isDeepLinked` on the UI state is known before items arrive; `highlightedItemId` only after. The
list must know to hold off following before it can know where to jump.

The highlight is a tinted band behind the whole item, not a span on the words: message bodies render
as markdown, whose only styling hook fires per syntax node, so a match spanning a space would go
unmarked. A band is exact and reads the same on a reply, a thought and a tool call.

## The error state

A failed run surfaces once as the `Error` state over the timeline. Retry clears it even when
`RetryChatUseCase` finds nothing to run, because an error about a run that is over would otherwise be
a screen with no way off it. The error screen offers a second button, open chat settings, because
the likeliest cause is a chat with no usable model, which no amount of retrying fixes.

## Thought preview

A collapsed thought that is still thinking shows its last three non-blank reasoning lines instead of
"Thinking…": the tail is what is changing, which turns a spinner into visible progress. A finished
or expanded thought keeps the label; its content no longer moves and is one tap away. Blank lines
are filtered before `takeLast`, or preview slots are spent on nothing.

## Prompt field and layout

- Bottom insets go on the prompt field, not the screen column; padding the whole screen would drag
  the top bar up with the keyboard. `imePadding` is applied first so the two insets do not stack.
- The send button sits beside the field, not in its `trailing` slot, which top-aligns in a multi-line
  field. Submit reads the text straight from the field because the debounced change callback may be
  behind, and the same lambda serves the button and the IME action.
- The options menu is anchored to the whole top bar: `ThoonTopBar` exposes a click lambda, not an
  anchor slot.

## Tool approval

A tool call the user has not yet allowed is its own list item, `ChatItem.ToolApproval`, rendered
under the tool-call row rather than folded into it: the row is a disclosure, and a disclosure that
also carries three buttons and a menu stops reading as one thing. While the chat is
`AwaitingApproval` the prompt field is blocked and says why. It is not the working state, so there
is no stop button; a prompt appended after an unanswered tool call is a request every provider
rejects, so the only ways forward are the decisions on the item. The mechanics, and why the run
ends instead of suspending inside the tool, are in `docs/architecture/AGENT_RUNTIME.md`.
