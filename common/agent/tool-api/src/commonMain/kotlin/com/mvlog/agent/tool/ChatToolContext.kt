package com.mvlog.agent.tool

/**
 * Which conversation a call belongs to.
 *
 * Every tool is scoped to one chat, and this is what scopes it — a file store keys its folder by
 * this, so passing the wrong one would let a conversation read another's files. It is a plain
 * String rather than `ChatId` so this module stays independent of `common:agent:api`.
 */
class ChatToolContext(val chatId: String)
