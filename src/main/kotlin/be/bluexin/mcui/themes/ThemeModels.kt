/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A Minecraft-style identifier kept free of client and renderer dependencies. */
data class ThemeId(val namespace: String, val path: String) {
    init {
        require(NAMESPACE.matches(namespace)) { "Invalid theme namespace '$namespace'" }
        require(PATH.matches(path)) { "Invalid theme path '$path'" }
    }

    override fun toString(): String = "$namespace:$path"

    companion object {
        private val NAMESPACE = Regex("[a-z0-9_.-]+")
        private val PATH = Regex("[a-z0-9/._-]+")

        fun parse(value: String): ThemeId? {
            val separator = value.indexOf(':')
            if (separator <= 0 || separator == value.lastIndex) return null
            return runCatching {
                ThemeId(value.substring(0, separator), value.substring(separator + 1))
            }.getOrNull()
        }
    }
}

/**
 * Superset of the 1.19.4 `theme.mcui.json` metadata.
 *
 * The historical fields remain parseable even when their runtime features are deferred.
 * Theme IDs are still derived from the resource path; [id] is an optional consistency check.
 */
@Serializable
data class ThemeMetadata(
    val format: String,
    val version: String = UNKNOWN_VERSION,
    val id: String? = null,
    val name: String? = null,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val website: String? = null,
    val supportedVersion: String? = null,
    val parent: String? = null,
    @SerialName("extends") val extendsTheme: String? = null,
    val namespace: String? = null,
    val fragments: String = "fragments",
    val widgets: String = "widgets",
    val scripts: String = "scripts",
) {
    companion object {
        const val UNKNOWN_VERSION = "unknown"
        const val RESOLVED_V1_FORMAT = "mcui:resolved-v1"
        const val LEGACY_ALPHA_FORMAT = "mcui:alpha"
    }
}

@Serializable
data class ThemeDocument(
    val version: String = "1",
    /** Phase-three compatibility overlay. It is rendered independently of HUD parts. */
    val root: ElementDefinition? = null,
    /** Historical HUD part names with the phase-four resolved element schema. */
    val parts: Map<HudPartType, ElementDefinition> = emptyMap(),
    /** Reusable subtrees resolved and instantiated during resource reload. */
    val fragments: Map<String, ElementDefinition> = emptyMap(),
)

@Serializable
data class TransformDefinition(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val scale: Double = 1.0,
    val anchor: HudAnchor = HudAnchor.TOP_LEFT,
)

/** A texture atlas region shared by textures, textured bars, and themed hotbar slots. */
@Serializable
data class TextureRegionDefinition(
    val texture: String,
    val u: Double = 0.0,
    val v: Double = 0.0,
    val sourceWidth: Int? = null,
    val sourceHeight: Int? = null,
    val textureWidth: Int = 256,
    val textureHeight: Int = 256,
    val tint: ArgbColorDefinition = ArgbColorDefinition.WHITE,
)

/**
 * Resolved-value element DTO. Type-specific requirements are enforced by ThemeCompiler,
 * keeping deserialization separate from renderer model construction.
 */
@Serializable
data class ElementDefinition(
    val type: String,
    val name: String? = null,
    val enabled: Boolean = true,
    val transform: TransformDefinition = TransformDefinition(),
    val children: List<ElementDefinition> = emptyList(),
    val fragment: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val color: ArgbColorDefinition? = null,
    val text: String? = null,
    val shadow: Boolean = false,
    val centered: Boolean = false,
    val texture: String? = null,
    val u: Double = 0.0,
    val v: Double = 0.0,
    val sourceWidth: Int? = null,
    val sourceHeight: Int? = null,
    val textureWidth: Int? = null,
    val textureHeight: Int? = null,
    val tint: ArgbColorDefinition? = null,
    val valueSource: HudValueSource? = null,
    val backgroundColor: ArgbColorDefinition? = null,
    val foregroundColor: ArgbColorDefinition? = null,
    val direction: ProgressDirection = ProgressDirection.LEFT_TO_RIGHT,
    val backgroundTexture: TextureRegionDefinition? = null,
    val foregroundTexture: TextureRegionDefinition? = null,
    val clip: Boolean = true,
    val textSource: HudTextSource? = null,
    val itemSource: HudItemSource? = null,
    val slotSize: Int = 20,
    val slotSpacing: Int = 0,
    val itemXOffset: Int = 2,
    val itemYOffset: Int = 2,
    val slotBackgroundColor: ArgbColorDefinition? = null,
    val selectedSlotColor: ArgbColorDefinition? = null,
    val slotTexture: TextureRegionDefinition? = null,
    val selectedSlotTexture: TextureRegionDefinition? = null,
    val orientation: HotbarOrientation = HotbarOrientation.HORIZONTAL,
    val decorations: Boolean = true,
    val effectRowHeight: Int = 22,
    val maxEffects: Int = 8,
    val beneficialColor: ArgbColorDefinition? = null,
    val harmfulColor: ArgbColorDefinition? = null,
    val showEffectDuration: Boolean = true,
    val showEffectIcons: Boolean = true,
)

data class ThemeDefinition(
    val id: ThemeId,
    val metadata: ThemeMetadata,
    val document: ThemeDocument,
    val metadataResource: String,
    val hudResource: String,
    val sourcePack: String,
)

enum class ThemeIssueSeverity { WARNING, ERROR }

data class ThemeIssue(
    val severity: ThemeIssueSeverity,
    val resource: String,
    val themeId: ThemeId?,
    val field: String,
    val message: String,
)

data class ThemeValidationResult(
    val issues: List<ThemeIssue>,
) {
    val isValid: Boolean get() = issues.none { it.severity == ThemeIssueSeverity.ERROR }
}
