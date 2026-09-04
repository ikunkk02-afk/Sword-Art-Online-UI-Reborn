/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes.legacy

import be.bluexin.mcui.themes.ThemeDocument
import be.bluexin.mcui.themes.ThemeId
import be.bluexin.mcui.themes.ThemeIssue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class LegacyThemeAdaptation(
    val document: ThemeDocument,
    val issues: List<ThemeIssue>,
)

/** Parses the 1.12 key-discriminator JSON format without restoring Gson. */
class LegacyJsonThemeLoader(
    private val json: Json = Json { ignoreUnknownKeys = true; allowTrailingComma = true; allowComments = true },
) {
    fun parseHud(content: String, themeId: ThemeId, resource: String): Result<LegacyThemeAdaptation> = runCatching {
        val root = json.parseToJsonElement(content).jsonObject
        LegacyThemeAdapter(themeId, resource).adapt(root)
    }

    fun parseFragment(content: String, themeId: ThemeId, resource: String): Result<LegacyThemeAdaptation> = runCatching {
        val root: JsonObject = json.parseToJsonElement(content).jsonObject
        LegacyThemeAdapter(themeId, resource).adaptFragment(root)
    }
}
