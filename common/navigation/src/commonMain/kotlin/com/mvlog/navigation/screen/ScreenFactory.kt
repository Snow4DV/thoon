package com.mvlog.navigation.screen

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui

/**
 * Both halves of one screen, handed over together.
 *
 * Bundled rather than registered separately because the state type a presenter produces and its UI
 * consumes is star-projected — `Presenter<*>` and `Ui<*>` — so the compiler never checks that they
 * agree. A feature that registered a presenter and forgot its UI would fail when someone navigated
 * to the screen, not when the app was built. Requiring both here makes that hard to get wrong.
 */
class ScreenFactory(
    val presenterFactory: Presenter.Factory,
    val uiFactory: Ui.Factory,
)
