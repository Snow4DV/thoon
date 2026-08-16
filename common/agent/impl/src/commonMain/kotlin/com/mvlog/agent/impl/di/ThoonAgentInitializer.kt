package com.mvlog.agent.impl.di

import com.mvlog.agent.api.di.ThoonAgentComponentHolder
import com.mvlog.init.BaseInitializer
import com.mvlog.init.start.AppOnCreateAction
import com.mvlog.init.start.AppOnCreateActionsCollector

/**
 * Wires the agent subsystem into the app.
 *
 * Deliberately does no work of its own: it registers a provider (the component itself is built on
 * first access) and collects one start-up action. The action only launches the run coordinator,
 * which returns immediately — it exists so a prompt left queued by a previous session resumes
 * without waiting for a screen to touch the holder.
 */
class ThoonAgentInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        ThoonAgentComponentHolder.set { ThoonAgentComponentImpl() }

        AppOnCreateActionsCollector.collect(
            AppOnCreateAction(tag = TAG) {
                ThoonAgentComponentHolder.get().startAgentRuntimeUseCase().invoke()
            }
        )
    }

    private companion object {
        const val TAG = "ThoonAgent"
    }
}
