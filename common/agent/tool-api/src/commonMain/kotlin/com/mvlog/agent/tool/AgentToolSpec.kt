package com.mvlog.agent.tool

/** The model-facing description; the framework adapter builds the schema from it. */
class AgentToolSpec(
    /** `snake_case`; stored in conversations — renaming orphans past calls. */
    val name: String,
    /** Written for the model, not the reader: it is the only guidance on when to reach for this. */
    val description: String,
    val parameters: List<AgentToolParameter> = emptyList(),
    /** The run stops and asks the user before this tool executes, unless an "always" rule matches. */
    val requiresApproval: Boolean = false,
)

class AgentToolParameter(
    val name: String,
    val description: String,
    val type: AgentToolParameterType = AgentToolParameterType.String,
    val isRequired: Boolean = true,
    /** An "always" approval remembers this parameter's value and covers only calls repeating it. */
    val requiresApprovalPerValue: Boolean = false,
)

/** Only what the adapter can map to a provider schema. */
enum class AgentToolParameterType {
    String,
    Integer,
    Boolean,
}
