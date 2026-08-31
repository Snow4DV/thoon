package com.mvlog.agent.api.engine

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalEnginesCollectorTest {

    @AfterTest
    fun tearDown() = LocalEnginesCollector.reset()

    @Test
    fun nothingIsAvailableUntilSomethingRegisters() {
        // The case that actually ships: no module implements an on-device engine, so the editor
        // must be able to say so rather than offering a choice that cannot work.
        assertTrue(LocalEnginesCollector.obtain().isEmpty())
    }

    @Test
    fun registeredEnginesAreReturnedAndReadingDoesNotDrain() {
        LocalEnginesCollector.collect { listOf(LocalEngine(id = "llama-cpp", displayName = "llama.cpp")) }

        assertEquals(listOf("llama-cpp"), LocalEnginesCollector.obtain().map { it.id })
        assertEquals(1, LocalEnginesCollector.obtain().size, "reading must not consume")
    }
}
