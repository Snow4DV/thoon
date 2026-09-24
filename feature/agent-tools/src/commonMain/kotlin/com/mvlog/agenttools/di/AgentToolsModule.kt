package com.mvlog.agenttools.di

import com.mvlog.agent.tool.AgentToolProvider
import com.mvlog.agenttools.file.ChatFileStore
import com.mvlog.agenttools.file.OkioChatFileStore
import com.mvlog.agenttools.file.platformFileSystem
import com.mvlog.agenttools.file.platformFilesRoot
import com.mvlog.agenttools.tools.EditFileTool
import com.mvlog.agenttools.tools.ListFilesTool
import com.mvlog.agenttools.tools.ReadFileTool
import com.mvlog.agenttools.tools.WriteFileTool
import com.mvlog.agenttools.time.CurrentDateTimeTool
import com.mvlog.agenttools.web.FetchUrlTool
import com.mvlog.agenttools.web.WebSearchTool

internal interface AgentToolsModule {

    val agentToolProvider: AgentToolProvider

    class Impl(
        dependencies: AgentToolsComponentDependencies,
    ) : AgentToolsModule, AgentToolsComponentDependencies by dependencies {

        /** `by lazy`: resolve the platform files root once, not per tool call. */
        private val fileStore: ChatFileStore by lazy {
            OkioChatFileStore(
                fileSystem = platformFileSystem(),
                root = platformFilesRoot(),
                dispatchers = dispatchers,
            )
        }

        override val agentToolProvider: AgentToolProvider
            get() = AgentToolProvider {
                listOf(
                    ListFilesTool(fileStore),
                    ReadFileTool(fileStore),
                    WriteFileTool(fileStore),
                    EditFileTool(fileStore),
                    FetchUrlTool(httpClient),
                    WebSearchTool(httpClient),
                    CurrentDateTimeTool(),
                )
            }
    }
}
