package com.mvlog.chat.di

import com.mvlog.navigation.screen.ScreenFactory

/**
 * What the chat feature hands to whoever assembles the app's `Circuit`.
 *
 * One value, not two: a presenter and its UI share a state type the compiler never checks —
 * `Presenter<*>` and `Ui<*>` are star-projected — so handing them over separately makes it possible
 * to register one without the other and only find out on navigation.
 */
internal interface ChatComponent {

    fun screenFactory(): ScreenFactory
}
