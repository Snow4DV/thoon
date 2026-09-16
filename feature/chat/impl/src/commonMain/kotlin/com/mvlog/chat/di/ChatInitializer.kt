package com.mvlog.chat.di

import com.mvlog.chat.api.ChatScreen
import com.mvlog.init.BaseInitializer
import com.mvlog.navigation.screen.ScreenFactoriesCollector

class ChatInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        ChatComponentHolder.set { ChatComponentImpl() }

        ScreenFactoriesCollector.collect(ChatScreen.serializer()) {
            ChatComponentHolder.get().screenFactory()
        }
    }

    private companion object {
        const val TAG = "Chat"
    }
}
