package com.mvlog.agenttools.di

import com.mvlog.agent.tool.AgentToolProvider
import com.mvlog.agenttools.file.ChatFileStore
import com.mvlog.agenttools.file.OkioChatFileStore
import com.mvlog.agenttools.file.platformFilesRoot
import com.mvlog.agenttools.tools.EditFileTool
import com.mvlog.agenttools.tools.ListFilesTool
import com.mvlog.agenttools.tools.ReadFileTool
import com.mvlog.agenttools.tools.WriteFileTool
import com.mvlog.agenttools.web.FetchUrlTool
import com.mvlog.agenttools.web.WebSearchTool
import okio.FileSystem

/**
 * Everything this feature builds for itself.
 */
internal interface AgentToolsModule {

    val agentToolProvider: AgentToolProvider

    class Impl(
        dependencies: AgentToolsComponentDependencies,
    ) : AgentToolsModule, AgentToolsComponentDependencies by dependencies {

        /**
         * Built once, not per access: [platformFilesRoot] reads a platform handle that must be
         * installed first, and resolving it repeatedly would ask the same question on every tool
         * call.
         */
        private val fileStore: ChatFileStore by lazy {
            OkioChatFileStore(
                fileSystem = FileSystem.SYSTEM,
                root = platformFilesRoot(),
                dispatchers = dispatchers,
            )
        }

        /**
         * Lazy by construction: the provider is invoked when a run starts, so nothing here touches
         * a filesystem or an HTTP client during process launch.
         */
        override val agentToolProvider: AgentToolProvider
            get() = AgentToolProvider {
                listOf(
                    ListFilesTool(fileStore),
                    ReadFileTool(fileStore),
                    WriteFileTool(fileStore),
                    EditFileTool(fileStore),
                    FetchUrlTool(httpClient),
                    WebSearchTool(httpClient),
                )
            }
    }
}
