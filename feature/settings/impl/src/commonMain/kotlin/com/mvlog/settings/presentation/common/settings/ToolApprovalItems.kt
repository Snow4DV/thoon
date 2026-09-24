package com.mvlog.settings.presentation.common.settings

import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import com.mvlog.settings.presentation.common.settings.ui.SettingItem

/**
 * One toggle per gated tool for its blanket rule, then one per parameterised rule, which can only
 * be switched off: a value-bound rule is made from a call, not from settings.
 */
internal fun toolApprovalItems(
    gatedTools: List<String>,
    rules: List<ToolApprovalRule>,
    scopeSubtitle: String,
    onAllow: (toolName: String) -> Unit,
    onRevoke: (ToolApprovalRuleId) -> Unit,
): List<SettingItem> = buildList {
    gatedTools.forEach { tool ->
        val blanket = rules.firstOrNull { it.toolName == tool && it.parameters.isEmpty() }
        add(
            SettingItem.Toggle(
                title = "Always allow $tool",
                subtitle = scopeSubtitle,
                isOn = blanket != null,
                onChange = { on -> if (on) onAllow(tool) else blanket?.let { onRevoke(it.id) } },
            ),
        )
    }

    rules.filter { it.parameters.isNotEmpty() }.forEach { rule ->
        add(
            SettingItem.Toggle(
                title = "Always allow ${rule.toolName}",
                subtitle = rule.parameters.entries.joinToString(" · ") { "${it.key} = ${it.value}" },
                isOn = true,
                onChange = { on -> if (!on) onRevoke(rule.id) },
            ),
        )
    }
}
