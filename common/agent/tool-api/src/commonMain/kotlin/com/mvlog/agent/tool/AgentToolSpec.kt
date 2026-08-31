package com.mvlog.agent.tool

/**
 * What a model is told about a tool before it decides to call one.
 *
 * Deliberately not a JSON schema: the schema a provider needs is the agent framework's concern, and
 * describing tools in the framework's vocabulary would put that framework in every module that
 * contributes one. This carries only what a description needs, and the adapter builds the schema.
 */
class AgentToolSpec(
    /** Stable, `snake_case`, and part of the durable conversation — a rename orphans past calls. */
    val name: String,
    /** Written for the model, not the reader: it is the only guidance on when to reach for this. */
    val description: String,
    val parameters: List<AgentToolParameter> = emptyList(),
)

class AgentToolParameter(
    val name: String,
    val description: String,
    val type: AgentToolParameterType = AgentToolParameterType.String,
    val isRequired: Boolean = true,
)

/**
 * The value shapes a tool argument may take.
 *
 * Small on purpose. Every current tool takes strings, and a type the adapter cannot faithfully turn
 * into a provider schema would be a promise the model cannot keep.
 */
enum class AgentToolParameterType {
    String,
    Integer,
    Boolean,
}
