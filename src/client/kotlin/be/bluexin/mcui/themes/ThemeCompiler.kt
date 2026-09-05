/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import be.bluexin.mcui.animation.Easing
import be.bluexin.mcui.animation.ResolvedAnimationSpec
import be.bluexin.mcui.animation.ResolvedHealthAnimationSpec
import be.bluexin.mcui.render.ArgbColor
import be.bluexin.mcui.render.ResolvedRenderState
import be.bluexin.mcui.render.ResolvedTransform
import be.bluexin.mcui.render.element.DynamicTextElement
import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.EffectListElement
import be.bluexin.mcui.render.element.EntityHealthListElement
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.HotbarElement
import be.bluexin.mcui.render.element.HudItemElement
import be.bluexin.mcui.render.element.LegacySaoEffectsElement
import be.bluexin.mcui.render.element.LegacySaoEntityHealthElement
import be.bluexin.mcui.render.element.LegacySaoHudElement
import be.bluexin.mcui.render.element.LegacySaoPartyElement
import be.bluexin.mcui.render.element.ProgressBarElement
import be.bluexin.mcui.render.element.ProgressTint
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import be.bluexin.mcui.render.element.TexturedProgressBarElement
import be.bluexin.mcui.render.element.TargetEntityHealthElement
import be.bluexin.mcui.render.element.TextureElement
import be.bluexin.mcui.render.element.TextureRegion
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
        definition.document.fragments.forEach { (name, fragment) ->
            if (name.isBlank()) {
                issues.error(definition, "fragments", "Fragment names must not be blank")
            } else {
                compileElement(definition, fragment, "fragments.$name", issues, listOf(name))
            }
        }
        val compiledRoot = definition.document.root?.let {
            compileElement(definition, it, "root", issues, emptyList())
        }
        val compiledParts = linkedMapOf<HudPartType, Element>()
        definition.document.parts.forEach { (part, element) ->
            compileElement(definition, element, "parts.$part", issues, emptyList())?.let {
                compiledParts[part] = it
            }
        }
        if (definition.document.root == null && definition.document.parts.isEmpty()) {
            issues.error(definition, "hud", "HUD must define root and/or at least one part")
        }
        val compiledScreens = compileScreenTheme(definition, issues)
        val validation = ThemeValidationResult(issues.toList())
        val theme = if (validation.isValid) {
            val resolvedHud = ResolvedHud(
                globalOverlay = compiledRoot,
                parts = compiledParts.toMap(),
            )
            ResolvedTheme(
                id = definition.id,
                metadata = definition.metadata,
                hud = resolvedHud,
                screens = compiledScreens,
                sourcePack = definition.sourcePack,
                sourceResource = definition.hudResource,
                elementCount = listOfNotNull(compiledRoot).sumOf(::countElements) +
                    compiledParts.values.sumOf(::countElements),
            )
        } else {
            null
        }
        return ThemeCompileResult(theme, validation)
    }

    private fun compileScreenTheme(
        definition: ThemeDefinition,
        issues: MutableList<ThemeIssue>,
    ): ResolvedScreenTheme {
        val document = definition.screenDocument ?: return ResolvedScreenTheme.FALLBACK
        val resource = definition.screenResource ?: definition.hudResource
        if (document.version != "1") {
            issues += ThemeIssue(
                ThemeIssueSeverity.WARNING,
                resource,
                definition.id,
                "screens.version",
                "Unsupported screen theme version '${document.version}'; using built-in screen style",
            )
            return ResolvedScreenTheme.FALLBACK
        }

        fun warn(field: String, message: String) {
            issues += ThemeIssue(ThemeIssueSeverity.WARNING, resource, definition.id, field, message)
        }

        val opacityValues = listOf(
            "menuBackground" to document.opacity.menuBackground,
            "worldOverlay" to document.opacity.worldOverlay,
            "panel" to document.opacity.panel,
            "disabled" to document.opacity.disabled,
            "icon" to document.opacity.icon,
        )
        if (opacityValues.any { (_, value) -> !value.isFinite() || value !in 0.0..1.0 }) {
            opacityValues.filter { (_, value) -> !value.isFinite() || value !in 0.0..1.0 }
                .forEach { (field, value) -> warn("screens.opacity.$field", "Opacity must be within 0..1, got $value") }
            return ResolvedScreenTheme.FALLBACK
        }

        val fallback = ScreenThemeDefinition()
        fun requiredTexture(raw: String, fallbackRaw: String, field: String): String {
            val location = ResourceLocation.tryParse(raw)
            if (location == null || !textureExists(location)) {
                warn(
                    "screens.textures.$field",
                    if (location == null) "Invalid texture '$raw'; using built-in fallback"
                    else "Texture '$location' is missing; using built-in fallback",
                )
                return fallbackRaw
            }
            return raw
        }

        val optionalBackground = document.textures.menuBackground?.let { raw ->
            val location = ResourceLocation.tryParse(raw)
            if (location == null || !textureExists(location)) {
                warn(
                    "screens.textures.menuBackground",
                    if (location == null) "Invalid texture '$raw'; using gradient background"
                    else "Texture '$location' is missing; using gradient background",
                )
                null
            } else raw
        }
        val normalized = document.copy(
            textures = document.textures.copy(
                menuBackground = optionalBackground,
                logo = requiredTexture(document.textures.logo, fallback.textures.logo, "logo"),
                profileBackground = requiredTexture(
                    document.textures.profileBackground,
                    fallback.textures.profileBackground,
                    "profileBackground",
                ),
                dialogBackground = requiredTexture(
                    document.textures.dialogBackground,
                    fallback.textures.dialogBackground,
                    "dialogBackground",
                ),
                slot = requiredTexture(document.textures.slot, fallback.textures.slot, "slot"),
                death = requiredTexture(document.textures.death, fallback.textures.death, "death"),
            ),
        )
        return ResolvedScreenTheme.fromDefinition(normalized)
    }

    private fun validateMetadata(definition: ThemeDefinition, issues: MutableList<ThemeIssue>) {
        val metadata = definition.metadata
        if (metadata.format !in SUPPORTED_FORMATS) {
            issues.metadataError(definition, "metadata.format", "Unsupported theme format '${metadata.format}'")
        } else if (metadata.format == ThemeMetadata.LEGACY_ALPHA_FORMAT) {
            issues.metadataWarning(
                definition,
                "metadata.format",
                "Legacy mcui:alpha metadata is accepted, but only the resolved JSON element subset is compiled",
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
        fragmentStack: List<String>,
    ): Element? {
        val transform = compileTransform(definition, element.transform, "$path.transform", issues) ?: return null
        val state = ResolvedRenderState(
            enabled = element.enabled,
            name = element.name ?: path,
            key = path,
            animations = compileAnimations(definition, element.animations, "$path.animations", issues),
        )
        return when (element.type.lowercase()) {
            "group" -> {
                val children = element.children.mapIndexedNotNull { index, child ->
                    compileElement(definition, child, "$path.children[$index]", issues, fragmentStack)
                }
                GroupElement(state, transform, children)
            }

            "fragment_reference", "fragment", "reference" -> compileFragmentReference(
                definition,
                element,
                path,
                state,
                transform,
                issues,
                fragmentStack,
            )

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
                val text = element.text
                val valueSource = element.valueSource
                val textSource = element.textSource
                val sourceCount = listOfNotNull(text, valueSource, textSource).size
                when {
                    sourceCount != 1 -> {
                        issues.error(definition, path, "Text must define exactly one of text, valueSource, or textSource")
                        null
                    }

                    text != null -> TextElement(
                        renderState = state,
                        transform = transform,
                        text = Component.literal(text),
                        color = ArgbColor((element.color ?: ArgbColorDefinition.WHITE).value),
                        shadow = element.shadow,
                        centered = element.centered,
                    )

                    valueSource != null -> DynamicTextElement(
                        renderState = state,
                        transform = transform,
                        valueSource = valueSource,
                        color = ArgbColor((element.color ?: ArgbColorDefinition.WHITE).value),
                        shadow = element.shadow,
                        centered = element.centered,
                    )

                    textSource != null -> DynamicTextElement(
                        renderState = state,
                        transform = transform,
                        textSource = textSource,
                        color = ArgbColor((element.color ?: ArgbColorDefinition.WHITE).value),
                        shadow = element.shadow,
                        centered = element.centered,
                    )

                    else -> {
                        issues.error(definition, path, "Text must define a source")
                        null
                    }
                }
            }

            "texture" -> compileTexture(definition, element, path, state, transform, issues)
            "progress", "progress_bar", "bar" -> compileProgressBar(
                definition,
                element,
                path,
                state,
                transform,
                issues,
            )
            "textured_progress", "textured_progress_bar", "texture_bar" -> compileTexturedProgressBar(
                definition,
                element,
                path,
                state,
                transform,
                issues,
            )

            "hud_item", "dynamic_item" -> {
                val source = element.itemSource ?: run {
                    issues.error(definition, "$path.itemSource", "Dynamic item source is required")
                    null
                }
                source?.let {
                    HudItemElement(
                        renderState = state,
                        transform = transform,
                        source = it,
                        decorations = element.decorations,
                    )
                }
            }

            "hotbar" -> compileHotbar(definition, element, path, state, transform, issues)
            "legacy_sao_hud" -> compileRequiredTexture(definition, element, path, issues)?.let {
                LegacySaoHudElement(state, transform, it)
            }
            "legacy_sao_effects" -> LegacySaoEffectsElement(state, transform)
            "legacy_sao_entity_health" -> compileRequiredTexture(definition, element, path, issues)?.let {
                LegacySaoEntityHealthElement(state, transform, it)
            }
            "legacy_sao_party" -> compileRequiredTexture(definition, element, path, issues)?.let {
                LegacySaoPartyElement(state, transform, it)
            }
            "effects", "effect_list" -> compileEffectList(definition, element, path, state, transform, issues)
            "entity_health_list", "nearby_entity_health" -> compileEntityHealthList(
                definition,
                element,
                path,
                state,
                transform,
                issues,
            )
            "target_entity_health", "target_health" -> compileTargetEntityHealth(
                definition,
                element,
                path,
                state,
                transform,
                issues,
            )
            else -> {
                issues.error(definition, "$path.type", "Unknown element type '${element.type}'")
                null
            }
        }
    }

    private fun compileProgressBar(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        val width = positiveDimension(definition, element.width, "$path.width", issues)
        val height = positiveDimension(definition, element.height, "$path.height", issues)
        val foreground = element.foregroundColor ?: run {
            issues.error(definition, "$path.foregroundColor", "Progress foregroundColor is required")
            null
        }
        val source = element.valueSource ?: run {
            issues.error(definition, "$path.valueSource", "Progress valueSource is required")
            null
        }
        if (width == null || height == null || foreground == null || source == null) return null
        return ProgressBarElement(
            renderState = state,
            transform = transform,
            width = width,
            height = height,
            backgroundColor = element.backgroundColor?.let { ArgbColor(it.value) },
            foregroundColor = ArgbColor(foreground.value),
            direction = element.direction,
            valueSource = source,
        )
    }

    private fun compileTexturedProgressBar(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        val width = positiveDimension(definition, element.width, "$path.width", issues)
        val height = positiveDimension(definition, element.height, "$path.height", issues)
        val foregroundDefinition = element.foregroundTexture ?: run {
            issues.error(definition, "$path.foregroundTexture", "Textured progress foregroundTexture is required")
            null
        }
        val source = element.valueSource ?: run {
            issues.error(definition, "$path.valueSource", "Textured progress valueSource is required")
            null
        }
        if (width == null || height == null || foregroundDefinition == null || source == null) return null
        val foreground = compileTextureRegion(
            definition,
            foregroundDefinition,
            "$path.foregroundTexture",
            width,
            height,
            issues,
        ) ?: return null
        val background = element.backgroundTexture?.let {
            compileTextureRegion(definition, it, "$path.backgroundTexture", width, height, issues)
        }
        val valueTints = element.valueTints.mapIndexedNotNull { index, step ->
            if (!step.maximum.isFinite() || step.maximum !in 0.0..1.0) {
                issues.error(definition, "$path.valueTints[$index].maximum", "Progress tint maximum must be in 0..1")
                null
            } else {
                ProgressTint(step.maximum.toFloat(), ArgbColor(step.tint.value))
            }
        }.sortedBy(ProgressTint::maximum)
        val delayedForeground = element.delayedForegroundTexture?.let {
            compileTextureRegion(definition, it, "$path.delayedForegroundTexture", width, height, issues)
        }
        val healthAnimation = element.healthAnimation?.let { animation ->
            if (source != HudValueSource.PLAYER_HEALTH) {
                issues.warning(definition, "$path.healthAnimation", "Health animation is only supported for PLAYER_HEALTH")
                null
            } else if (animation.easing == AnimationEasing.UNSUPPORTED) {
                issues.warning(definition, "$path.healthAnimation.easing", "Unsupported health easing; animation deferred")
                null
            } else if (
                animation.mainDuration < 0 || animation.damageDelay < 0 || animation.damageDuration < 0 ||
                animation.healDuration < 0
            ) {
                issues.warning(definition, "$path.healthAnimation", "Negative health animation timing is unsupported; animation deferred")
                null
            } else {
                ResolvedHealthAnimationSpec(
                    mainDurationMillis = animation.mainDuration,
                    damageDelayMillis = animation.damageDelay,
                    damageDurationMillis = animation.damageDuration,
                    healDurationMillis = animation.healDuration,
                    easing = Easing.resolve(animation.easing),
                )
            }
        }
        return TexturedProgressBarElement(
            renderState = state,
            transform = transform,
            width = width,
            height = height,
            background = background,
            foreground = foreground,
            direction = element.direction,
            valueSource = source,
            clip = element.clip,
            valueTints = valueTints,
            creativeTint = element.creativeTint?.let { ArgbColor(it.value) },
            delayedForeground = delayedForeground,
            healthAnimation = healthAnimation,
        )
    }

    private fun compileFragmentReference(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
        fragmentStack: List<String>,
    ): Element? {
        val requested = element.fragment?.trim().orEmpty()
        if (requested.isEmpty()) {
            issues.error(definition, "$path.fragment", "Fragment reference is required")
            return null
        }
        val key = when {
            requested in definition.document.fragments -> requested
            requested.substringAfterLast(':') in definition.document.fragments -> requested.substringAfterLast(':')
            else -> requested
        }
        val fragment = definition.document.fragments[key]
        if (fragment == null) {
            issues.error(definition, "$path.fragment", "Missing fragment '$requested'")
            return null
        }
        if (key in fragmentStack) {
            val cycle = (fragmentStack + key).joinToString(" -> ")
            issues.error(definition, "$path.fragment", "Circular fragment reference: $cycle")
            return null
        }
        val instantiated = applyFragmentArguments(fragment, element.fragmentArguments)
        val resolved = compileElement(
            definition,
            instantiated,
            "$path.fragment[$key]",
            issues,
            fragmentStack + key,
        ) ?: return null
        return GroupElement(state, transform, listOf(resolved))
    }

    private fun applyFragmentArguments(
        fragment: ElementDefinition,
        arguments: FragmentArguments,
    ): ElementDefinition {
        val transform = fragment.transform.copy(
            x = arguments.x ?: fragment.transform.x,
            y = arguments.y ?: fragment.transform.y,
            z = arguments.z ?: fragment.transform.z,
            scale = arguments.scale ?: fragment.transform.scale,
        )
        return fragment.copy(
            enabled = arguments.enabled ?: fragment.enabled,
            transform = transform,
            color = arguments.color ?: fragment.color,
            text = arguments.text ?: fragment.text,
            texture = arguments.texture ?: fragment.texture,
            tint = arguments.tint ?: fragment.tint,
        )
    }

    private fun compileHotbar(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        if (element.slotSize <= 0) {
            issues.error(definition, "$path.slotSize", "Hotbar slotSize must be greater than zero")
            return null
        }
        if (element.slotSpacing < 0) {
            issues.error(definition, "$path.slotSpacing", "Hotbar slotSpacing must not be negative")
            return null
        }
        if (element.offhandGap < 0) {
            issues.error(definition, "$path.offhandGap", "Hotbar offhandGap must not be negative")
            return null
        }
        return HotbarElement(
            renderState = state,
            transform = transform,
            slotSize = element.slotSize,
            slotSpacing = element.slotSpacing,
            itemXOffset = element.itemXOffset,
            itemYOffset = element.itemYOffset,
            slotBackgroundColor = element.slotBackgroundColor?.let { ArgbColor(it.value) },
            selectedSlotColor = element.selectedSlotColor?.let { ArgbColor(it.value) },
            slotTexture = element.slotTexture?.let {
                compileTextureRegion(definition, it, "$path.slotTexture", element.slotSize, element.slotSize, issues)
            },
            selectedSlotTexture = element.selectedSlotTexture?.let {
                compileTextureRegion(
                    definition,
                    it,
                    "$path.selectedSlotTexture",
                    element.slotSize,
                    element.slotSize,
                    issues,
                )
            },
            selectionReplacesSlot = element.selectionReplacesSlot,
            orientation = element.orientation,
            decorations = element.decorations,
            showOffhand = element.showOffhand,
            offhandGap = element.offhandGap,
        )
    }

    private fun compileEffectList(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        val width = positiveDimension(definition, element.width, "$path.width", issues) ?: return null
        if (element.effectRowHeight <= 0) {
            issues.error(definition, "$path.effectRowHeight", "Effect row height must be greater than zero")
            return null
        }
        if (element.effectIconSize <= 0) {
            issues.error(definition, "$path.effectIconSize", "Effect icon size must be greater than zero")
            return null
        }
        if (element.showEffectIcons && element.effectRowHeight < element.effectIconSize) {
            issues.error(
                definition,
                "$path.effectRowHeight",
                "Effect row height must be at least effectIconSize when icons are enabled",
            )
            return null
        }
        if (element.effectRowHeight + element.effectSpacing <= 0) {
            issues.error(definition, "$path.effectSpacing", "Effect rowHeight + effectSpacing must be positive")
            return null
        }
        if (element.maxEffects <= 0) {
            issues.error(definition, "$path.maxEffects", "Effect maxEffects must be greater than zero")
            return null
        }
        return EffectListElement(
            renderState = state,
            transform = transform,
            width = width,
            rowHeight = element.effectRowHeight,
            maxEffects = element.maxEffects,
            backgroundColor = element.backgroundColor?.let { ArgbColor(it.value) },
            textColor = ArgbColor((element.foregroundColor ?: ArgbColorDefinition.WHITE).value),
            beneficialColor = ArgbColor(element.beneficialColor?.value ?: DEFAULT_BENEFICIAL_COLOR),
            harmfulColor = ArgbColor(element.harmfulColor?.value ?: DEFAULT_HARMFUL_COLOR),
            showDuration = element.showEffectDuration,
            showIcons = element.showEffectIcons,
            showLabels = element.showEffectLabels,
            orientation = element.effectOrientation,
            spacing = element.effectSpacing,
            iconSize = element.effectIconSize,
            iconSet = element.effectIconSet,
            includePlayerStates = element.includePlayerStates,
            entryAnimations = compileAnimations(
                definition,
                element.entryAnimations,
                "$path.entryAnimations",
                issues,
            ),
        )
    }

    private fun compileTargetEntityHealth(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        val width = positiveDimension(definition, element.width, "$path.width", issues) ?: return null
        val height = positiveDimension(definition, element.height ?: element.entityRowHeight, "$path.height", issues)
            ?: return null
        val backgroundDefinition = element.backgroundTexture ?: run {
            issues.error(definition, "$path.backgroundTexture", "Target health background texture is required")
            return null
        }
        val foregroundDefinition = element.foregroundTexture ?: run {
            issues.error(definition, "$path.foregroundTexture", "Target health foreground texture is required")
            return null
        }
        if (element.targetLinger < 0) {
            issues.warning(definition, "$path.targetLinger", "Negative target linger is unsupported; using 3000ms")
        }
        return TargetEntityHealthElement(
            renderState = state,
            transform = transform,
            width = width,
            height = height,
            background = compileTextureRegion(definition, backgroundDefinition, "$path.backgroundTexture", width, height, issues)
                ?: return null,
            foreground = compileTextureRegion(definition, foregroundDefinition, "$path.foregroundTexture", width, height, issues)
                ?: return null,
            textColor = ArgbColor((element.foregroundColor ?: ArgbColorDefinition.WHITE).value),
            lingerMillis = if (element.targetLinger < 0) 3000 else element.targetLinger,
        )
    }

    private fun compileEntityHealthList(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        state: ResolvedRenderState,
        transform: ResolvedTransform,
        issues: MutableList<ThemeIssue>,
    ): Element? {
        val width = positiveDimension(definition, element.width, "$path.width", issues) ?: return null
        if (element.entityRowHeight <= 0) {
            issues.error(definition, "$path.entityRowHeight", "Entity row height must be greater than zero")
            return null
        }
        if (element.maxEntities <= 0) {
            issues.error(definition, "$path.maxEntities", "Entity maxEntities must be greater than zero")
            return null
        }
        val backgroundDefinition = element.backgroundTexture ?: run {
            issues.error(definition, "$path.backgroundTexture", "Entity health background texture is required")
            return null
        }
        val foregroundDefinition = element.foregroundTexture ?: run {
            issues.error(definition, "$path.foregroundTexture", "Entity health foreground texture is required")
            return null
        }
        val background = compileTextureRegion(
            definition,
            backgroundDefinition,
            "$path.backgroundTexture",
            width,
            element.entityRowHeight,
            issues,
        ) ?: return null
        val foreground = compileTextureRegion(
            definition,
            foregroundDefinition,
            "$path.foregroundTexture",
            width,
            element.entityRowHeight,
            issues,
        ) ?: return null
        return EntityHealthListElement(
            renderState = state,
            transform = transform,
            width = width,
            rowHeight = element.entityRowHeight,
            maxEntities = element.maxEntities,
            background = background,
            foreground = foreground,
            textColor = ArgbColor((element.foregroundColor ?: ArgbColorDefinition.WHITE).value),
        )
    }

    private fun compileRequiredTexture(
        definition: ThemeDefinition,
        element: ElementDefinition,
        path: String,
        issues: MutableList<ThemeIssue>,
    ): ResourceLocation? {
        val rawTexture = element.texture ?: run {
            issues.error(definition, "$path.texture", "Texture resource location is required")
            return null
        }
        val texture = ResourceLocation.tryParse(rawTexture)
        if (texture == null) {
            issues.error(definition, "$path.texture", "Invalid ResourceLocation '$rawTexture'")
            return null
        }
        if (!textureExists(texture)) {
            issues.warning(definition, "$path.texture", "Texture '$texture' does not exist in the active resource stack")
        }
        return texture
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

    private fun compileTextureRegion(
        definition: ThemeDefinition,
        region: TextureRegionDefinition,
        path: String,
        defaultWidth: Int,
        defaultHeight: Int,
        issues: MutableList<ThemeIssue>,
    ): TextureRegion? {
        val texture = ResourceLocation.tryParse(region.texture)
        if (texture == null) {
            issues.error(definition, "$path.texture", "Invalid ResourceLocation '${region.texture}'")
            return null
        }
        val sourceWidth = positiveDimension(
            definition,
            region.sourceWidth ?: defaultWidth,
            "$path.sourceWidth",
            issues,
        ) ?: return null
        val sourceHeight = positiveDimension(
            definition,
            region.sourceHeight ?: defaultHeight,
            "$path.sourceHeight",
            issues,
        ) ?: return null
        if (region.textureWidth <= 0 || region.textureHeight <= 0) {
            issues.error(definition, path, "Texture atlas dimensions must be greater than zero")
            return null
        }
        if (!region.u.isFinite() || !region.v.isFinite()) {
            issues.error(definition, path, "Texture coordinates must be finite")
            return null
        }
        if (!textureExists(texture)) {
            issues.warning(definition, "$path.texture", "Texture '$texture' does not exist in the active resource stack")
        }
        return TextureRegion(
            texture = texture,
            u = region.u.toFloat(),
            v = region.v.toFloat(),
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            textureWidth = region.textureWidth,
            textureHeight = region.textureHeight,
            tint = ArgbColor(region.tint.value),
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
            anchor = value.anchor,
        )
    }

    private fun compileAnimations(
        definition: ThemeDefinition,
        animations: List<AnimationDefinition>,
        path: String,
        issues: MutableList<ThemeIssue>,
    ): List<ResolvedAnimationSpec> = animations.mapIndexedNotNull { index, animation ->
        val field = "$path[$index]"
        when {
            animation.property == AnimationProperty.UNSUPPORTED ||
                animation.trigger == AnimationTrigger.UNSUPPORTED ||
                animation.easing == AnimationEasing.UNSUPPORTED -> {
                issues.warning(definition, field, "Unsupported animation property, trigger, or easing; animator deferred")
                null
            }
            animation.duration < 0 || animation.delay < 0 -> {
                issues.warning(definition, field, "Negative duration/delay is unsupported; animator deferred")
                null
            }
            animation.from?.isFinite() == false || animation.to?.isFinite() == false -> {
                issues.warning(definition, field, "Animation endpoints must be finite literal values; animator deferred")
                null
            }
            animation.property == AnimationProperty.COLOR && (animation.from == null || animation.to == null) -> {
                issues.warning(definition, field, "COLOR animation requires literal packed-ARGB from and to values")
                null
            }
            animation.property != AnimationProperty.PROGRESS &&
                animation.trigger == AnimationTrigger.ON_VALUE_CHANGE &&
                animation.property !in setOf(AnimationProperty.TRANSLATION_X, AnimationProperty.TRANSLATION_Y) -> {
                issues.warning(definition, field, "Unsupported ON_VALUE_CHANGE property '${animation.property}'; animator deferred")
                null
            }
            else -> ResolvedAnimationSpec(
                property = animation.property,
                durationMillis = animation.duration,
                delayMillis = animation.delay,
                easing = Easing.resolve(animation.easing),
                from = animation.from,
                to = animation.to,
                trigger = animation.trigger,
            )
        }
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
        private const val DEFAULT_BENEFICIAL_COLOR = -11141291
        private const val DEFAULT_HARMFUL_COLOR = -43691
        private val SUPPORTED_FORMATS = setOf(
            ThemeMetadata.RESOLVED_V1_FORMAT,
            ThemeMetadata.LEGACY_ALPHA_FORMAT,
        )
    }
}
