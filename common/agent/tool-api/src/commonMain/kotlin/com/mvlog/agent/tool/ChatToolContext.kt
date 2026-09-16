package com.mvlog.agent.tool

/**
 * Scopes a call to one chat — the file sandbox keys on it. A String, not `ChatId`, so tool-api does
 * not depend on `common:agent:api`.
 */
class ChatToolContext(val chatId: String)
