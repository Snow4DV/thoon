package com.mvlog.agent.impl

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.impl.data.mapper.AgentConfigMapper
import com.mvlog.agent.impl.data.serialization.AgentConfigKind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Nothing else encodes a config: the fixture is in-memory, so a wrong @SerialName only shows on a
 * real reopen.
 */
class AgentConfigMapperTest {

    private val mapper = AgentConfigMapper(Json)

    @Test
    fun ollamaSurvivesARoundTripThroughStorage() {
        val id = AgentConfigId("cfg-1")
        val entity = mapper.toEntity(
            id = id,
            draft = AgentConfigDraft.Ollama(
                name = "Debug",
                modelId = "qwen3.8:27b",
                baseUrl = "http://10.0.2.2:11434",
            ),
        )

        assertEquals(
            AgentConfigKind.OLLAMA,
            entity.kind,
            "the kind column is what lets configurations be queried without parsing every payload",
        )

        val config = assertIs<AgentConfig.Ollama>(mapper.toDomain(entity))
        assertEquals(id, config.id)
        assertEquals("Debug", config.name)
        assertEquals("qwen3.8:27b", config.modelId)
        assertEquals("http://10.0.2.2:11434", config.baseUrl)
    }

    @Test
    fun eachProtocolDecodesBackAsItself() {
        // One discriminator shared by two variants would decode as whichever is listed first.
        val drafts = listOf<AgentConfigDraft>(
            AgentConfigDraft.OpenAiCompatible(name = "A", modelId = "m", apiKey = "k"),
            AgentConfigDraft.Anthropic(name = "B", modelId = "m", apiKey = "k"),
            AgentConfigDraft.Ollama(name = "C", modelId = "m", baseUrl = "http://host:11434"),
            AgentConfigDraft.Local(name = "D", modelId = "m", engineId = "e"),
        )

        val decoded = drafts.map { draft ->
            mapper.toDomain(mapper.toEntity(AgentConfigId("id"), draft))
        }

        assertIs<AgentConfig.OpenAiCompatible>(decoded[0])
        assertIs<AgentConfig.Anthropic>(decoded[1])
        assertIs<AgentConfig.Ollama>(decoded[2])
        assertIs<AgentConfig.Local>(decoded[3])
    }
}
