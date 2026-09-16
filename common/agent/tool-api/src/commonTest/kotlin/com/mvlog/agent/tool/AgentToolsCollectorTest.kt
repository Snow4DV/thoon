package com.mvlog.agent.tool

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentToolsCollectorTest {

    @AfterTest
    fun tearDown() = AgentToolsCollector.reset()

    @Test
    fun collectedProvidersSurviveBeingRead() {
        AgentToolsCollector.collect { emptyList() }

        assertEquals(1, AgentToolsCollector.collected().size)
        assertEquals(1, AgentToolsCollector.collected().size, "reading must not consume")
    }

    @Test
    fun collectedIsASnapshot() {
        AgentToolsCollector.collect { emptyList() }
        val first = AgentToolsCollector.collected()

        AgentToolsCollector.collect { emptyList() }

        assertEquals(1, first.size, "an earlier snapshot must not change under the caller")
        assertEquals(2, AgentToolsCollector.collected().size)
    }
}
