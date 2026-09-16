package com.mvlog.agent.api.usecase

/**
 * Call once at startup; until then queued prompts are persisted but never run. Returns immediately;
 * repeat calls are harmless.
 */
fun interface StartAgentRuntimeUseCase {
    operator fun invoke()
}
