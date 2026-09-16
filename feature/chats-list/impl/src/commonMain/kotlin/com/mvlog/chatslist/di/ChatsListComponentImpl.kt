package com.mvlog.chatslist.di

import com.mvlog.navigation.screen.ScreenFactory

internal class ChatsListComponentImpl(
    dependencies: ChatsListComponentDependencies = ChatsListComponentDependencies.Impl(),
    private val module: ChatsListModule = ChatsListModule.Impl(dependencies),
) : ChatsListComponent {

    override fun screenFactory(): ScreenFactory = module.screenFactory
}
