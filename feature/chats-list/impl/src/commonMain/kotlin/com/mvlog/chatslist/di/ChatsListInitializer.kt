package com.mvlog.chatslist.di

import com.mvlog.chatslist.api.ChatsListScreen
import com.mvlog.init.BaseInitializer
import com.mvlog.navigation.screen.ScreenFactoriesCollector

class ChatsListInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        ChatsListComponentHolder.set { ChatsListComponentImpl() }

        ScreenFactoriesCollector.collect(ChatsListScreen.serializer()) {
            ChatsListComponentHolder.get().screenFactory()
        }
    }

    private companion object {
        const val TAG = "ChatsList"
    }
}
