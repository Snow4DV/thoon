package com.mvlog.chat.di

import com.mvlog.chat.api.ChatScreen
import com.mvlog.init.BaseInitializer
import com.mvlog.navigation.screen.ScreenFactoriesCollector

/**
 * Wires the chat feature into the app.
 *
 * The registered lambda does not run here. It runs the first time something navigates to
 * [ChatScreen], which is what keeps the component — and through it the agent subsystem — out of
 * process launch.
 */
class ChatInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        ChatComponentHolder.set { ChatComponentImpl() }

        ScreenFactoriesCollector.collect<ChatScreen> { ChatComponentHolder.get().screenFactory() }
    }

    private companion object {
        const val TAG = "Chat"
    }
}
