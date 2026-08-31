package com.mvlog.chat.di

import com.mvlog.navigation.screen.ScreenFactory

/**
 * Exposes the module's screen factory.
 *
 * Builds nothing itself — that belongs to [ChatModule].
 */
internal class ChatComponentImpl(
    dependencies: ChatComponentDependencies = ChatComponentDependencies.Impl(),
    private val module: ChatModule = ChatModule.Impl(dependencies),
) : ChatComponent {

    override fun screenFactory(): ScreenFactory = module.screenFactory
}
