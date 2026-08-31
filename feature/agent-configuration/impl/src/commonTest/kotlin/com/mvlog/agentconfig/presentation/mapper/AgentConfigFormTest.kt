package com.mvlog.agentconfig.presentation.mapper

import com.mvlog.agent.api.model.AgentConfigDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * One flat form produces four differently shaped drafts.
 *
 * The form deliberately keeps what was typed under a previously selected protocol, so the risk is
 * that something irrelevant is carried into storage — an API key on an Ollama endpoint, an address
 * on an on-device engine.
 */
class AgentConfigFormTest {

    private val filled = AgentConfigForm(
        name = "Local",
        modelId = "qwen3.8:27b",
        apiKey = "typed-under-another-protocol",
        baseUrl = "http://10.0.2.2:11434",
        chatCompletionsPath = "v1/chat/completions",
        engineId = "llama-cpp",
    )

    @Test
    fun ollamaTakesItsAddressAndCarriesNoKey() {
        val draft = assertIs<AgentConfigDraft.Ollama>(filled.copy(kind = ConfigKind.Ollama).toDraft())

        assertEquals("http://10.0.2.2:11434", draft.baseUrl)
        assertEquals("qwen3.8:27b", draft.modelId)
        // AgentConfigDraft.Ollama has no apiKey at all; the leftover must not reach storage by
        // some other route either.
        assertEquals("Local", draft.name)
    }

    @Test
    fun anOnDeviceEngineTakesNoAddress() {
        val draft = assertIs<AgentConfigDraft.Local>(filled.copy(kind = ConfigKind.Local).toDraft())

        assertEquals("llama-cpp", draft.engineId)
    }

    @Test
    fun anEmptyOptionalBecomesNullRatherThanBlank() {
        // A blank base URL means "use the provider default", and storing "" would be a URL that
        // validates as absent but reads as present.
        val draft = assertIs<AgentConfigDraft.OpenAiCompatible>(
            filled.copy(kind = ConfigKind.OpenAiCompatible, baseUrl = "", chatCompletionsPath = "")
                .toDraft(),
        )

        assertNull(draft.baseUrl)
        assertNull(draft.chatCompletionsPath)
    }

    @Test
    fun editingAFieldLeavesTheSelectedProtocolAlone() {
        // The bug this pins: typing in any field reset the protocol to OpenAI-compatible. The cause
        // was upstream — a Compose effect writing onto a form captured before the protocol changed —
        // but the editor now builds every edit through `write`, so this is the contract that keeps
        // it honest.
        val ollama = filled.copy(kind = ConfigKind.Ollama)

        val edited = ollama.write(ConfigField.ModelId, "qwen3.8:27b")

        assertEquals(ConfigKind.Ollama, edited.kind)
        assertEquals("qwen3.8:27b", edited.modelId)
    }

    @Test
    fun writingOneFieldChangesNothingElse() {
        val edited = filled.write(ConfigField.Name, "Renamed")

        assertEquals(filled.copy(name = "Renamed"), edited)
    }

    @Test
    fun everyFieldRoundTripsThroughReadAndWrite() {
        // The editor reads a field to show it and writes it back on every keystroke, so a read and
        // a write that disagreed about which property they mean would swap two inputs silently.
        ConfigField.entries.forEach { field ->
            assertEquals(field.name, filled.write(field, field.name).read(field))
        }
    }

    @Test
    fun aLocalAddressTypedWithoutASchemeGetsHttp() {
        // The commonest thing anyone does here: point the app at a server on their own machine.
        // 10.0.2.2 is the emulator's alias for the host, and Ollama serves plain HTTP.
        val draft = assertIs<AgentConfigDraft.Ollama>(
            filled.copy(kind = ConfigKind.Ollama, baseUrl = "10.0.2.2:11434").toDraft(),
        )

        assertEquals("http://10.0.2.2:11434", draft.baseUrl)
    }

    @Test
    fun everyPrivateRangeIsTreatedAsLocal() {
        listOf("localhost:11434", "127.0.0.1:1234", "192.168.1.5:1234", "172.16.0.9", "nas.local")
            .forEach { typed ->
                val draft = assertIs<AgentConfigDraft.Ollama>(
                    filled.copy(kind = ConfigKind.Ollama, baseUrl = typed).toDraft(),
                )
                assertEquals("http://$typed", draft.baseUrl, "$typed should be treated as local")
            }
    }

    @Test
    fun aRoutableAddressGetsHttps() {
        val draft = assertIs<AgentConfigDraft.Anthropic>(
            filled.copy(kind = ConfigKind.Anthropic, baseUrl = "api.anthropic.com").toDraft(),
        )

        assertEquals("https://api.anthropic.com", draft.baseUrl)
    }

    @Test
    fun anExplicitSchemeIsLeftAlone() {
        // Including someone deliberately serving a routable host over plain HTTP.
        listOf("http://example.com", "https://10.0.2.2:11434").forEach { typed ->
            val draft = assertIs<AgentConfigDraft.Ollama>(
                filled.copy(kind = ConfigKind.Ollama, baseUrl = typed).toDraft(),
            )
            assertEquals(typed, draft.baseUrl)
        }
    }

    @Test
    fun aBlankAddressStaysBlankRatherThanBecomingAScheme() {
        val draft = assertIs<AgentConfigDraft.OpenAiCompatible>(
            filled.copy(kind = ConfigKind.OpenAiCompatible, baseUrl = "  ").toDraft(),
        )

        assertNull(draft.baseUrl, "blank means 'use the provider default', not 'https://'")
    }

    @Test
    fun eachProtocolShowsOnlyTheInputsItUses() {
        assertEquals(
            listOf(ConfigField.Name, ConfigField.ModelId, ConfigField.BaseUrl),
            filled.copy(kind = ConfigKind.Ollama).visibleFields,
            "an Ollama endpoint has no API key to ask for",
        )
        assertEquals(
            listOf(ConfigField.Name, ConfigField.ModelId, ConfigField.Engine),
            filled.copy(kind = ConfigKind.Local).visibleFields,
            "an on-device engine has no address",
        )
    }
}
