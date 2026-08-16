package com.mvlog.init.start

object AppOnCreateActionsCollector {

    private val actions = mutableListOf<AppOnCreateAction>()

    /**
     * Should only be invoked in base initializers!
     */
    fun collect(action: AppOnCreateAction) {
        actions.add(action)
    }

    fun execute() {
        actions.toList().also { actions.clear() }.forEach { it.action() }
    }
}
