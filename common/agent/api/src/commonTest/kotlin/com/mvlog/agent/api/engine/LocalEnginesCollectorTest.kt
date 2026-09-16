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
        assertTrue(LocalEnginesCollector.obtain().isEmpty())
    }

    @Test
    fun registeredEnginesAreReturnedAndReadingDoesNotDrain() {
        LocalEnginesCollector.collect { listOf(LocalEngine(id = "llama-cpp", displayName = "llama.cpp")) }

        assertEquals(listOf("llama-cpp"), LocalEnginesCollector.obtain().map { it.id })
        assertEquals(1, LocalEnginesCollector.obtain().size, "reading must not consume")
    }
}
