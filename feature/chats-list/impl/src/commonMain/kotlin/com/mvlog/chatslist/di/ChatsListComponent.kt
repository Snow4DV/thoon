package com.mvlog.chatslist.di

import com.mvlog.navigation.screen.ScreenFactory

/**
 * What the chats-list feature hands to whoever assembles the app's `Circuit`.
 */
internal interface ChatsListComponent {

    fun screenFactory(): ScreenFactory
}
