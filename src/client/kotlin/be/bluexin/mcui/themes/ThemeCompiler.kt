/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import be.bluexin.mcui.render.ArgbColor
import be.bluexin.mcui.render.ResolvedRenderState
import be.bluexin.mcui.render.ResolvedTransform
import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import be.bluexin.mcui.render.element.TextureElement
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

data class ThemeCompileResult(
    val theme: ResolvedTheme?,
    val validation: ThemeValidationResult,
)

class ThemeCompiler(
    private val textureExists: (ResourceLocation) -> Boolean,
) {
    fun compile(definition: ThemeDefinition): ThemeCompileResult {
        val issues = mutableListOf<ThemeIssue>()
        validateMetadata(definition, issues)
        val compiledRoot = compileElement(definition, definition.document.root, "root", issues)
        val validation = ThemeValidationResult(issues.toList())
        val theme = if (compiledRoot != null && validation.isValid) {
            ResolvedTheme(
                id = definition.id,
                metadata = definition.metadata,
                hudRoot = compiledRoot,
                sourcePack = definition.sourcePack,
                sourceResource = definition.hudResource,
                elementCount = countElements(compiledRoot),
            )
        } else {
            null
        }
        return ThemeCompileResult(theme, validation)
    }

    private fun validateMetadata(definition: ThemeDefinition, issues: MutableList<ThemeIssue>) {
        val metadata = definition.metadata
        if (metadata.format !in SUPPORTED_FORMATS) {
            issues.metadataError(definition, "metadata.format", "Unsupported theme format '${metadata.format}'")
        } else if (metadata.format == ThemeMetadata.LEGACY_ALPHA_FORMAT) {
            issues.metadataWarning(
                definition,
                "metadata.format",
                "Legacy mcui:alpha metadata is accepted, but only the phase-three resolved JSON element subset is compiled",
            )
        } else {
            if (metadata.name == null) {
                issues.metadataError(definition, "metadata.name", "Name is required for mcui:resolved-v1 themes")
            }
            if (metadata.version == ThemeMetadata.UNKNOWN_VERSION) {
                issues.metadataError(definition, "metadata.version", "Version is required for mcui:resolved-v1 themes")
            }
            if (metadata.authors.isEmpty()) {
                issues.metadataError(definition, "metadata.authors", "At least one author is required for mcui:resolved-v1 themes")
            }
        }

        metadata.id?.let { declared ->
            val parsed = ThemeId.parse(declared)
            when {
                parsed == null -> issues.metadataError(definition, "metadata.id", "Invalid theme id '$declared'")
                parsed != definition.id -> issues.metadataError(
                    definition,
                    "metadata.id",
                    "Declared id '$parsed' does not match resource-derived id '${definition.id}'",
                )
            }
        }
        metadata.name?.let { name ->
            if (name.isBlank()) issues.metadataError(definition, "metadata.name", "Theme name must not be blank")
        }
        if (metadata.version.isBlank()) {
            issues.metadataError(definition, "metadata.version", "Theme version must not be blank")
        }
        metadata.authors.forEachIndexed { index, author ->
            if (author.isBlank()) issues.metadataError(definition, "metadata.authors[$index]", "Author must not be blank")
        }
        metadata.namespace?.let { namespace ->
            if (ThemeId.parse("$namespace:probe") == null) {
                issues.metadataError(definition, "metadata.namespace", "Invalid namespace '$namespace'")
            }
        }
        listOf("parent" to metadata.parent, "extends" to metadata.extendsTheme).forEach { (field, value) ->
            value?.let {
                if (ThemeId.parse(it) == null) {
                    issues.metadataError(definition, "metadata.$field", "Invalid theme reference '$it'")
                } else {
                    issues.metadataWarning(definition, "metadata.$field", "Theme inheritance is parsed but deferred")
                }
            }
        }
    }

    private fun compileElement(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        val transform = compileTransform(definition, element.transform, "$path.transform", issues) ?: return null
        val state = ResolvedRenderState(enabled = element.enabled, name = element.name ?: path)
        return when (element.type.lowercase()) {
            "group" -> {
                val children = element.children.mapIndexedNotNull { index, child ->
                    compileElement(definition, child, "$path.children[$index]", issues)
                }
                GroupElement(state, transform, children)
            }

            "rectangle" -> {
                val width = positiveDimension(definition, element.width, "$path.width", issues)
                val height = positiveDimension(definition, element.height, "$path.height", issues)
                val color = element.color ?: run {
                    issues.error(definition, "$path.color", "Rectangle color is required")
                    null
                }
                if (width == null || height == null || color == null) null
                else RectangleElement(state, transform, width, height, ArgbColor(color.value))
            }

            "text" -> {
                val text = element.text ?: run {
                    issues.error(definition, "$path.text", "Text value is required")
                    null
                }
                text?.let {
                    TextElement(
                        renderState = state,
                        transform = transform,
                        text = Component.literal(it),
                        color = ArgbColor((element.color ?: ArgbColorDefinition.WHITE).value),
                        shadow = element.shadow,
                        centered = element.centered,
                    )
                }
            }

            "texture" -> compileTexture(definition, element, path, state, transform, issues)
            else -> {
                issues.error(definition, "$path.type", "Unknown element type '${element.type}'")
                null
            }
        }
    }

    private fun compileTexture(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        val rawTexture = element.texture ?: run {
            issues.error(definition, "$path.texture", "Texture resource location is required")
            return null
        }
        val texture = ResourceLocation.tryParse(rawTexture)
        if (texture == null) {
            issues.error(definition, "$path.texture", "Invalid ResourceLocation '$rawTexture'")
            return null
        }
        val width = positiveDimension(definition, element.width, "$path.width", issues) ?: return null
        val height = positiveDimension(definition, element.height, "$path.height", issues) ?: return null
        val sourceWidth = positiveDimension(
            definition,
            element.sourceWidth ?: width,
            "$path.sourceWidth",
            issues,
        ) ?: return null
        val sourceHeight = positiveDimension(
            definition,
            element.sourceHeight ?: height,
            "$path.sourceHeight",
            issues,
        ) ?: return null
        val textureWidth = positiveDimension(
            definition,
            element.textureWidth ?: 256,
            "$path.textureWidth",
            issues,
        ) ?: return null
        val textureHeight = positiveDimension(
            definition,
            element.textureHeight ?: 256,
            "$path.textureHeight",
            issues,
        ) ?: return null
        if (!element.u.isFinite() || !element.v.isFinite()) {
            issues.error(definition, path, "Texture coordinates must be finite")
            return null
        }
        if (!textureExists(texture)) {
            issues.warning(definition, "$path.texture", "Texture '$texture' does not exist in the active resource stack")
        }
        return TextureElement(
            renderState = state,
            transform = transform,
            texture = texture,
            width = width,
            height = height,
            u = element.u.toFloat(),
            v = element.v.toFloat(),
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            textureWidth = textureWidth,
            textureHeight = textureHeight,
            tint = ArgbColor((element.tint ?: ArgbColorDefinition.WHITE).value),
        )
    }

    private fun compileTransform(
        definition: ThemeDefinition,
        value: TransformDefinition,
        path: String,
        issues: MutableList<ThemeIssue>,
    ): ResolvedTransform? {
        if (!value.x.isFinite() || !value.y.isFinite() || !value.z.isFinite() || !value.scale.isFinite()) {
            issues.error(definition, path, "Transform values must be finite")
            return null
        }
        if (value.scale <= 0.0) {
            issues.error(definition, "$path.scale", "Scale must be greater than zero")
            return null
        }
        return ResolvedTransform(
            x = value.x.toFloat(),
            y = value.y.toFloat(),
            z = value.z.toFloat(),
            scaleX = value.scale.toFloat(),
            scaleY = value.scale.toFloat(),
        )
    }

    private fun positiveDimension(
        definition: ThemeDefinition,
        value: Int?,
        field: String,
        issues: MutableList<ThemeIssue>,
    ): Int? {
        if (value == null) {
            issues.error(definition, field, "Required dimension is missing")
            return null
        }
        if (value <= 0) {
            issues.error(definition, field, "Dimension must be greater than zero, got $value")
            return null
        }
        return value
    }

    private fun countElements(element: Element): Int = 1 + when (element) {
        is GroupElement -> element.children.sumOf(::countElements)
        else -> 0
    }

    private fun MutableList<ThemeIssue>.error(definition: ThemeDefinition, field: String, message: String) {
        add(ThemeIssue(ThemeIssueSeverity.ERROR, definition.hudResource, definition.id, field, message))
    }

    private fun MutableList<ThemeIssue>.warning(definition: ThemeDefinition, field: String, message: String) {
        add(ThemeIssue(ThemeIssueSeverity.WARNING, definition.hudResource, definition.id, field, message))
    }

    private fun MutableList<ThemeIssue>.metadataError(
        definition: ThemeDefinition,
        field: String,
        message: String,
    ) {
        add(ThemeIssue(ThemeIssueSeverity.ERROR, definition.metadataResource, definition.id, field, message))
    }

    private fun MutableList<ThemeIssue>.metadataWarning(
        definition: ThemeDefinition,
        field: String,
        message: String,
    ) {
        add(ThemeIssue(ThemeIssueSeverity.WARNING, definition.metadataResource, definition.id, field, message))
    }

    companion object {
        private val SUPPORTED_FORMATS = setOf(
            ThemeMetadata.RESOLVED_V1_FORMAT,
            ThemeMetadata.LEGACY_ALPHA_FORMAT,
        )
    }
}
