package com.mvlog.chatslist.di

import com.mvlog.chatslist.api.ChatsListScreen
import com.mvlog.init.BaseInitializer
import com.mvlog.navigation.screen.ScreenFactoriesCollector

/**
 * Wires the chats-list feature into the app.
 *
 * The registered lambda does not run here — it runs the first time something navigates to
 * [ChatsListScreen], which for the root screen is the first composition.
 */
class ChatsListInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        ChatsListComponentHolder.set { ChatsListComponentImpl() }

        ScreenFactoriesCollector.collect<ChatsListScreen> {
            ChatsListComponentHolder.get().screenFactory()
        }
    }

    private companion object {
        const val TAG = "ChatsList"
    }
}
