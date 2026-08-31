package com.mvlog.chatslist.di

import com.mvlog.navigation.screen.ScreenFactory

/**
 * Exposes the module's screen factory.
 *
 * Builds nothing itself — that belongs to [ChatsListModule].
 */
internal class ChatsListComponentImpl(
    dependencies: ChatsListComponentDependencies = ChatsListComponentDependencies.Impl(),
    private val module: ChatsListModule = ChatsListModule.Impl(dependencies),
) : ChatsListComponent {

    override fun screenFactory(): ScreenFactory = module.screenFactory
}
