/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ThemeJsonParser(
    private val json: Json = DEFAULT_JSON,
) {
    fun parseMetadata(content: String): Result<ThemeMetadata> = decode(content)

    fun parseDocument(content: String): Result<ThemeDocument> = decode(content)

    fun parseScreens(content: String): Result<ScreenThemeDefinition> = decode(content)

    private inline fun <reified T> decode(content: String): Result<T> = try {
        Result.success(json.decodeFromString<T>(content))
    } catch (cause: Exception) {
        Result.failure(SerializationException(cause.message ?: "Malformed JSON", cause))
    }

    companion object {
        val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = false
            allowTrailingComma = true
            allowComments = true
        }
    }
}
