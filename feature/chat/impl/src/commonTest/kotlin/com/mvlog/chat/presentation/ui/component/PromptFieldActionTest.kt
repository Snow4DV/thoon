package com.mvlog.chat.presentation.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class PromptFieldActionTest {

    @Test
    fun aWorkingAgentOffersStopEvenWithADraftReady() {
        assertEquals(
            PromptFieldAction.Stop,
            promptFieldAction(isWorking = true, canSend = true),
            "sending mid-run is what lost prompts; stop must take the slot",
        )
    }

    @Test
    fun anIdleAgentWithTextCanSend() {
        assertEquals(PromptFieldAction.Send, promptFieldAction(isWorking = false, canSend = true))
    }

    @Test
    fun anIdleAgentWithNothingTypedCannotSend() {
        assertEquals(PromptFieldAction.SendDisabled, promptFieldAction(isWorking = false, canSend = false))
    }
}
