package com.mvlog.chat.di

import com.mvlog.navigation.screen.ScreenFactory

internal interface ChatComponent {

    fun screenFactory(): ScreenFactory
}
