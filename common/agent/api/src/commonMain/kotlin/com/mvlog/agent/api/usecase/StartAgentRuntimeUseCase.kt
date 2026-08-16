package com.mvlog.agent.api.usecase

/**
 * Starts executing queued prompts.
 *
 * Call once during app start-up. Prompts are only *persisted* when submitted — nothing runs them
 * until this has been called, so without it a prompt left queued by a previous session would wait
 * until something else happened to touch the agent subsystem.
 *
 * Returns immediately: the work is handed to a background scope. Calling it more than once is
 * harmless.
 */
fun interface StartAgentRuntimeUseCase {
    operator fun invoke()
}
