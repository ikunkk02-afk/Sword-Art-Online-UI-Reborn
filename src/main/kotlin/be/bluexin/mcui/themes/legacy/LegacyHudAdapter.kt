/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes.legacy

import be.bluexin.mcui.themes.ElementDefinition
import be.bluexin.mcui.themes.HudPartType
import be.bluexin.mcui.themes.ThemeId
import be.bluexin.mcui.themes.ThemeIssue
import be.bluexin.mcui.themes.ThemeIssueSeverity
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Converts the historical parts map while keeping unknown names visible as warnings. */
class LegacyHudAdapter(
    private val themeId: ThemeId,
    private val resource: String,
    private val issues: MutableList<ThemeIssue>,
    private val elements: LegacyElementAdapter,
) {
    fun adapt(parts: JsonObject, path: String): Map<HudPartType, ElementDefinition> = buildMap {
        parts.forEach { (rawName, value) ->
            val partPath = "$path.$rawName"
            val part = LegacyPartNameMapper.map(rawName)
            if (part == null) {
                warning(partPath, "Unknown legacy HUD part '$rawName'")
                return@forEach
            }
            val definition = runCatching { elements.adaptGroup(value.jsonObject, partPath, null) }.getOrNull()
            if (definition == null || definition.children.isEmpty()) {
                warning(partPath, "Legacy HUD part '$rawName' has no supported static elements; Vanilla remains enabled")
            } else {
                put(part, definition)
            }
        }
    }

    private fun warning(field: String, message: String) {
        issues += ThemeIssue(ThemeIssueSeverity.WARNING, resource, themeId, field, message)
    }
}
