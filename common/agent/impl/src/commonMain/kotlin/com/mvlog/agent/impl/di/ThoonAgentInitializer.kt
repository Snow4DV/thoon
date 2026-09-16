package com.mvlog.agent.impl.di

import com.mvlog.agent.api.di.ThoonAgentComponentHolder
import com.mvlog.init.BaseInitializer
import com.mvlog.init.start.AppOnCreateAction
import com.mvlog.init.start.AppOnCreateActionsCollector

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
