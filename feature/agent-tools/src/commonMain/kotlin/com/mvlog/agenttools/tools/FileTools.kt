package com.mvlog.agenttools.tools

import com.mvlog.agent.tool.AgentToolParameter
import com.mvlog.agent.tool.AgentToolSpec
import com.mvlog.agent.tool.ChatToolContext
import com.mvlog.agent.tool.ThoonAgentTool
import com.mvlog.agenttools.file.ChatFileStore
import kotlinx.serialization.json.JsonObject

/**
 * The per-chat file tools.
 *
 * Descriptions are written for the model rather than for a reader of this file: they are the only
 * thing telling it that these files persist, and that they belong to one conversation.
 */
internal class ListFilesTool(private val store: ChatFileStore) : ThoonAgentTool {

    override val spec = AgentToolSpec(
        name = "list_files",
        description = "List the files stored for this conversation, with their sizes in bytes.",
    )

    override suspend fun execute(context: ChatToolContext, arguments: JsonObject): String {
        val files = store.list(context.chat)
        if (files.isEmpty()) return "This conversation has no files yet."

        return files.joinToString("\n") { "${it.path} (${it.sizeBytes} bytes)" }
    }
}

internal class ReadFileTool(private val store: ChatFileStore) : ThoonAgentTool {

    override val spec = AgentToolSpec(
        name = "read_file",
        description = "Read a text file stored for this conversation.",
        parameters = listOf(
            AgentToolParameter(
                name = "path",
                description = "Path relative to this conversation's folder, e.g. 'notes.md'.",
            ),
        ),
    )

    override suspend fun execute(context: ChatToolContext, arguments: JsonObject): String =
        store.read(context.chat, arguments.requireString("path"))
}

internal class WriteFileTool(private val store: ChatFileStore) : ThoonAgentTool {

    override val spec = AgentToolSpec(
        name = "write_file",
        description =
            "Create a text file, or replace one entirely. Parent folders are created as needed. " +
                "To change part of an existing file, use edit_file instead.",
        parameters = listOf(
            AgentToolParameter(
                name = "path",
                description = "Path relative to this conversation's folder, e.g. 'notes.md'.",
            ),
            AgentToolParameter(
                name = "content",
                description = "The file's complete new contents.",
            ),
        ),
    )

    override suspend fun execute(context: ChatToolContext, arguments: JsonObject): String {
        val path = arguments.requireString("path")
        val content = arguments.requireString("content")
        store.write(context.chat, path, content)
        return "Wrote ${content.length} characters to '$path'."
    }
}

internal class EditFileTool(private val store: ChatFileStore) : ThoonAgentTool {

    override val spec = AgentToolSpec(
        name = "edit_file",
        description =
            "Replace one exact passage of a file with another. old_text must appear exactly once, " +
                "so include enough surrounding text to identify it. Returns the edited region.",
        parameters = listOf(
            AgentToolParameter(
                name = "path",
                description = "Path relative to this conversation's folder.",
            ),
            AgentToolParameter(
                name = "old_text",
                description = "The exact text to replace. Must appear exactly once in the file.",
            ),
            AgentToolParameter(
                name = "new_text",
                description = "The text to put in its place.",
            ),
        ),
    )

    override suspend fun execute(context: ChatToolContext, arguments: JsonObject): String {
        val result = store.edit(
            chatId = context.chat,
            path = arguments.requireString("path"),
            oldText = arguments.requireString("old_text"),
            newText = arguments.requireString("new_text"),
        )

        // The excerpt, not just an acknowledgement: it is how the model confirms the edit landed
        // where it meant, and how an accidental double-apply becomes visible instead of silent.
        return "Edited '${result.path}'. The file now reads:\n\n${result.excerpt}"
    }
}
