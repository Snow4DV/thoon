package com.mvlog.agentconfig.presentation.mapper

import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Validation is typed so a rejection can land under the input that caused it. This is what decides
 * whether the user reads "An API key is required" beside the API key box or hunts for it.
 */
class AgentConfigErrorFieldTest {

    @Test
    fun everyFieldErrorPointsAtItsOwnInput() {
        assertEquals(ConfigField.Name, AgentConfigError.BlankName.field())
        assertEquals(ConfigField.ModelId, AgentConfigError.BlankModelId.field())
        assertEquals(ConfigField.ApiKey, AgentConfigError.MissingApiKey.field())
        assertEquals(ConfigField.BaseUrl, AgentConfigError.MissingBaseUrl.field())
        assertEquals(ConfigField.BaseUrl, AgentConfigError.MalformedBaseUrl("nope").field())
        assertEquals(ConfigField.Engine, AgentConfigError.BlankEngineId.field())
    }

    @Test
    fun aMissingConfigurationBelongsToNoField() {
        // Nothing the user typed is wrong, so putting this under an input would be a lie.
        assertNull(AgentConfigError.NotFound(AgentConfigId("gone")).field())
    }

    @Test
    fun aMalformedUrlQuotesWhatWasTyped() {
        assertTrue("10.0.2.2" in AgentConfigError.MalformedBaseUrl("10.0.2.2").message())
    }
}
