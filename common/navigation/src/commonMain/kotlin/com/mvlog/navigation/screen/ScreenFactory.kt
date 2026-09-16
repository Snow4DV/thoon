package com.mvlog.navigation.screen

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui

// Bundled so a feature cannot register a presenter and forget its UI: Presenter<*> and Ui<*> are
// star-projected.
class ScreenFactory(
    val presenterFactory: Presenter.Factory,
    val uiFactory: Ui.Factory,
)
