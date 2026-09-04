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
import be.bluexin.mcui.themes.ThemeDocument
import be.bluexin.mcui.themes.ThemeId
import be.bluexin.mcui.themes.ThemeIssue
import be.bluexin.mcui.themes.ThemeIssueSeverity
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Coordinates legacy HUD, fragment, and element conversion into modern definitions. */
class LegacyThemeAdapter(
    private val themeId: ThemeId,
    private val resource: String,
) {
    private val issues = mutableListOf<ThemeIssue>()
    private val elements = LegacyElementAdapter(themeId, resource, issues)
    private val hud = LegacyHudAdapter(themeId, resource, issues, elements)

    fun adapt(root: JsonObject): LegacyThemeAdaptation {
        val version = root["version"]?.jsonPrimitive?.contentOrNull ?: "legacy"
        val parts = root["parts"]?.let { value ->
            runCatching { hud.adapt(value.jsonObject, "parts") }.getOrElse {
                error("Legacy parts must be a JSON object: ${it.message}")
            }
        } ?: error("Legacy HUD is missing required 'parts'")
        val fragments = root["fragments"]?.let { value ->
            runCatching { adaptFragments(value.jsonObject) }.getOrElse {
                warning("fragments", "Legacy fragments could not be read: ${it.message}")
                emptyMap()
            }
        } ?: emptyMap()
        return LegacyThemeAdaptation(ThemeDocument(version = version, parts = parts, fragments = fragments), issues)
    }

    fun adaptFragment(root: JsonObject): LegacyThemeAdaptation {
        val definition = elements.adaptGroup(root, "fragment", null)
            ?: error("Legacy fragment has no supported elements")
        return LegacyThemeAdaptation(
            ThemeDocument(version = "legacy", fragments = mapOf("fragment" to definition)),
            issues,
        )
    }

    private fun adaptFragments(root: JsonObject): Map<String, ElementDefinition> = buildMap {
        root.forEach { (name, value) ->
            val fragment = elements.adaptGroup(value.jsonObject, "fragments.$name", null)
            if (fragment == null) warning("fragments.$name", "Fragment contains no supported elements")
            else put(name, fragment)
        }
    }

    private fun warning(field: String, message: String) {
        issues += ThemeIssue(ThemeIssueSeverity.WARNING, resource, themeId, field, message)
    }
}
