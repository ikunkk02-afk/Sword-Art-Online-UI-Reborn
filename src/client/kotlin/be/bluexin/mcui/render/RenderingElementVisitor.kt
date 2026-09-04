/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

import be.bluexin.mcui.animation.AnimatedRenderState
import be.bluexin.mcui.animation.AnimationRegistry
import be.bluexin.mcui.animation.EffectVisualSnapshot
import be.bluexin.mcui.fabric.client.hud.HudBindingResolver
import be.bluexin.mcui.fabric.client.hud.HudDataSnapshot
import be.bluexin.mcui.fabric.client.hud.HudEffectSnapshot
import be.bluexin.mcui.render.element.DynamicTextElement
import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.EffectListElement
import be.bluexin.mcui.render.element.EntityHealthListElement
import be.bluexin.mcui.render.element.ElementVisitor
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.HotbarElement
import be.bluexin.mcui.render.element.HudItemElement
import be.bluexin.mcui.render.element.ItemElement
import be.bluexin.mcui.render.element.LegacySaoEffectsElement
import be.bluexin.mcui.render.element.LegacySaoEntityHealthElement
import be.bluexin.mcui.render.element.LegacySaoHudElement
import be.bluexin.mcui.render.element.ProgressBarElement
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import be.bluexin.mcui.render.element.TexturedProgressBarElement
import be.bluexin.mcui.render.element.TargetEntityHealthElement
import be.bluexin.mcui.render.element.TextureElement
import be.bluexin.mcui.render.element.TextureRegion
import be.bluexin.mcui.themes.HudAnchor
import be.bluexin.mcui.themes.HudEffectIconSet
import be.bluexin.mcui.themes.HotbarOrientation
import be.bluexin.mcui.themes.ProgressDirection
import net.minecraft.network.chat.Component
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Traverses resolved elements and is the only layer allowed to issue render operations. */
class RenderingElementVisitor(
    private val operations: GuiRenderOperations,
    private val hudData: HudDataSnapshot? = null,
    private val animations: AnimationRegistry = AnimationRegistry(),
) : ElementVisitor {
    fun render(root: Element, context: RenderContext) {
        root.accept(this, context)
    }

    override fun visit(element: GroupElement, context: RenderContext) = withElement(element, context) { childContext ->
        element.children.sortedBy { it.transform.z }.forEach { it.accept(this, childContext) }
    }

    override fun visit(element: RectangleElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        operations.fill(0, 0, element.width, element.height, renderColor(element.color, animatedContext))
    }

    override fun visit(element: TextElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        operations.text(element.text, 0, 0, renderColor(element.color, animatedContext), element.shadow, element.centered)
    }

    override fun visit(element: TextureElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        operations.texture(
            texture = element.texture,
            x = 0,
            y = 0,
            width = element.width,
            height = element.height,
            u = element.u,
            v = element.v,
            sourceWidth = element.sourceWidth,
            sourceHeight = element.sourceHeight,
            textureWidth = element.textureWidth,
            textureHeight = element.textureHeight,
            tint = renderColor(element.tint, animatedContext),
        )
    }

    override fun visit(element: ItemElement, context: RenderContext) = withElement(element, context) { _ ->
        operations.item(element.stack, 0, 0, element.decorations, element.countText)
    }

    override fun visit(element: ProgressBarElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        element.backgroundColor?.let { operations.fill(0, 0, element.width, element.height, renderColor(it, animatedContext)) }
        val rawValue = HudBindingResolver.progress(element.valueSource, data, animations.mountSnapshot())
        val value = animations.progressValue(element.renderState.key, rawValue, element.renderState.animations)
        val foreground = renderColor(element.foregroundColor, animatedContext)
        when (element.direction) {
            ProgressDirection.LEFT_TO_RIGHT -> {
                val filled = (element.width * value).roundToInt()
                operations.fill(0, 0, filled, element.height, foreground)
            }

            ProgressDirection.RIGHT_TO_LEFT -> {
                val filled = (element.width * value).roundToInt()
                operations.fill(element.width - filled, 0, filled, element.height, foreground)
            }

            ProgressDirection.TOP_TO_BOTTOM -> {
                val filled = (element.height * value).roundToInt()
                operations.fill(0, 0, element.width, filled, foreground)
            }

            ProgressDirection.BOTTOM_TO_TOP -> {
                val filled = (element.height * value).roundToInt()
                operations.fill(0, element.height - filled, element.width, filled, foreground)
            }
        }
    }

    override fun visit(element: TexturedProgressBarElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        element.background?.let { drawTextureRegion(it, 0, 0, element.width, element.height, animatedContext) }
        val rawValue = HudBindingResolver.progress(element.valueSource, data, animations.mountSnapshot())
        val healthValue = element.healthAnimation?.let {
            animations.healthValue(element.renderState.key, rawValue, it)
        }
        val value = healthValue?.displayRatio
            ?: animations.progressValue(element.renderState.key, rawValue, element.renderState.animations)
        val dynamicTint = when {
            data.creative && element.creativeTint != null -> element.creativeTint
            else -> element.valueTints.firstOrNull { value <= it.maximum }?.tint
        }
        val foreground = dynamicTint?.let { element.foreground.copy(tint = it) } ?: element.foreground

        if (healthValue?.showDelayedDamage == true && element.delayedForeground != null) {
            drawProgressTexture(element.delayedForeground, element, healthValue.delayedRatio, animatedContext)
        }
        drawProgressTexture(foreground, element, value, animatedContext)
    }

    override fun visit(element: DynamicTextElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        operations.text(
            element.textSource?.let { HudBindingResolver.text(it, data, animations.mountSnapshot()) }
                ?: element.valueSource?.let { HudBindingResolver.text(it, data, animations.mountSnapshot()) }
                ?: return@withElement,
            0,
            0,
            renderColor(element.color, animatedContext),
            element.shadow,
            element.centered,
        )
    }

    override fun visit(element: HudItemElement, context: RenderContext) = withElement(element, context) { _ ->
        val data = hudData ?: return@withElement
        operations.item(
            HudBindingResolver.item(element.source, data),
            0,
            0,
            element.decorations,
            element.countText,
        )
    }

    override fun visit(element: HotbarElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        val stride = element.slotSize + element.slotSpacing
        data.hotbarItems.forEachIndexed { index, _ ->
            val offset = index * stride
            val x = if (element.orientation == HotbarOrientation.HORIZONTAL) offset else 0
            val y = if (element.orientation == HotbarOrientation.VERTICAL) offset else 0
            if (!element.selectionReplacesSlot || index != data.selectedHotbarSlot) {
                drawHotbarBackground(element, x, y, animatedContext)
            }
        }

        val selectedOffset = (animations.hotbarPosition(
            element.renderState.key,
            data.selectedHotbarSlot,
            element.renderState.animations,
        ) * stride).roundToInt()
        val selectedX = if (element.orientation == HotbarOrientation.HORIZONTAL) selectedOffset else 0
        val selectedY = if (element.orientation == HotbarOrientation.VERTICAL) selectedOffset else 0
        drawHotbarSelection(element, selectedX, selectedY, animatedContext)

        data.hotbarItems.forEachIndexed { index, stack ->
            val offset = index * stride
            val x = if (element.orientation == HotbarOrientation.HORIZONTAL) offset else 0
            val y = if (element.orientation == HotbarOrientation.VERTICAL) offset else 0
            drawHotbarItem(element, stack, x, y, data.hotbarItemPopTimes.getOrElse(index) { 0 }, data.partialTick)
        }

        if (element.showOffhand && !data.offHandItem.isEmpty) {
            val offset = data.hotbarItems.size * stride + element.offhandGap
            val x = if (element.orientation == HotbarOrientation.HORIZONTAL) offset else 0
            val y = if (element.orientation == HotbarOrientation.VERTICAL) offset else 0
            if (!element.selectionReplacesSlot) drawHotbarBackground(element, x, y, animatedContext)
            drawHotbarSelection(element, x, y, animatedContext)
            drawHotbarItem(element, data.offHandItem, x, y, data.offHandItemPopTime, data.partialTick)
        }
    }

    override fun visit(element: LegacySaoHudElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        val font = Minecraft.getInstance().font
        val usernameWidth = font.width(data.playerName) + LegacySaoHudMetrics.USERNAME_WIDTH_PADDING
        val smoothHealth = animations.legacyHealthValue(
            "${element.renderState.key}:health",
            data.playerHealth,
            data.playerMaxHealth,
            data.partialTick,
            data.dead,
        )
        val healthRatio = (smoothHealth / data.playerMaxHealth).coerceIn(0f, 1f)
        val healthTint = when {
            data.creative -> LegacySaoHudMetrics.HP_CREATIVE
            healthRatio <= LegacySaoHudMetrics.HEALTH_VERY_LOW_THRESHOLD -> LegacySaoHudMetrics.HP_VERY_LOW
            healthRatio <= LegacySaoHudMetrics.HEALTH_LOW_THRESHOLD -> LegacySaoHudMetrics.HP_LOW
            healthRatio <= LegacySaoHudMetrics.HEALTH_VERY_DAMAGED_THRESHOLD -> LegacySaoHudMetrics.HP_VERY_DAMAGED
            healthRatio <= LegacySaoHudMetrics.HEALTH_DAMAGED_THRESHOLD -> LegacySaoHudMetrics.HP_DAMAGED
            healthRatio <= LegacySaoHudMetrics.HEALTH_OKAY_THRESHOLD -> LegacySaoHudMetrics.HP_OKAY
            else -> LegacySaoHudMetrics.HP_GOOD
        }

        drawLegacyTexture(
            element.texture, 0, 0,
            LegacySaoHudMetrics.FRAME_LEFT_WIDTH, LegacySaoHudMetrics.FRAME_HEIGHT,
            0f, 0f,
            LegacySaoHudMetrics.FRAME_LEFT_WIDTH, LegacySaoHudMetrics.FRAME_HEIGHT,
            LegacySaoHudMetrics.WHITE, animatedContext,
        )
        drawLegacyTexture(
            element.texture, LegacySaoHudMetrics.FRAME_LEFT_WIDTH, 0,
            usernameWidth, LegacySaoHudMetrics.FRAME_HEIGHT,
            LegacySaoHudMetrics.FRAME_STRETCH_U.toFloat(), 0f,
            LegacySaoHudMetrics.FRAME_STRETCH_SOURCE_WIDTH, LegacySaoHudMetrics.FRAME_HEIGHT,
            LegacySaoHudMetrics.WHITE, animatedContext,
        )
        drawLegacyTexture(
            element.texture, LegacySaoHudMetrics.FRAME_LEFT_WIDTH + usernameWidth, 0,
            LegacySaoHudMetrics.FRAME_TAIL_WIDTH, LegacySaoHudMetrics.FRAME_HEIGHT,
            LegacySaoHudMetrics.FRAME_TAIL_U.toFloat(), 0f,
            LegacySaoHudMetrics.FRAME_TAIL_WIDTH, LegacySaoHudMetrics.FRAME_HEIGHT,
            LegacySaoHudMetrics.WHITE, animatedContext,
        )
        operations.text(
            data.playerName,
            LegacySaoHudMetrics.USERNAME_X,
            LegacySaoHudMetrics.USERNAME_Y,
            renderColor(LegacySaoHudMetrics.WHITE, animatedContext),
            shadow = true,
            centered = false,
        )

        val healthWidth = (LegacySaoHudMetrics.PLAYER_BAR_MAX_WIDTH * healthRatio).roundToInt()
        if (healthWidth > 0) {
            drawLegacyTexture(
                element.texture,
                LegacySaoHudMetrics.PLAYER_BAR_X + usernameWidth,
                LegacySaoHudMetrics.HEALTH_BAR_Y,
                healthWidth,
                LegacySaoHudMetrics.PLAYER_BAR_HEIGHT,
                LegacySaoHudMetrics.PLAYER_BAR_U.toFloat(),
                LegacySaoHudMetrics.PLAYER_BAR_V.toFloat(),
                healthWidth,
                LegacySaoHudMetrics.PLAYER_BAR_HEIGHT,
                healthTint,
                animatedContext,
            )
        }

        if (!data.creative && data.underwater && data.air < data.maxAir) {
            val airRatio = (data.air.toFloat() / data.maxAir).coerceIn(0f, 1f)
            val airWidth = (LegacySaoHudMetrics.PLAYER_BAR_MAX_WIDTH * airRatio).roundToInt()
            if (airWidth > 0) {
                drawLegacyTexture(
                    element.texture,
                    LegacySaoHudMetrics.PLAYER_BAR_X + usernameWidth,
                    LegacySaoHudMetrics.HEALTH_BAR_Y,
                    airWidth,
                    LegacySaoHudMetrics.PLAYER_BAR_HEIGHT,
                    LegacySaoHudMetrics.PLAYER_BAR_U.toFloat(),
                    LegacySaoHudMetrics.PLAYER_BAR_V.toFloat(),
                    airWidth,
                    LegacySaoHudMetrics.PLAYER_BAR_HEIGHT,
                    LegacySaoHudMetrics.AIR,
                    animatedContext,
                )
            }
        }

        if (!data.creative) {
            val smoothFood = animations.legacyFoodValue(
                "${element.renderState.key}:food",
                data.food.toFloat(),
                data.partialTick,
            )
            val foodRatio = (smoothFood / data.maxFood).coerceIn(0f, 1f)
            val foodWidth = (LegacySaoHudMetrics.FOOD_BAR_MAX_WIDTH * foodRatio).roundToInt()
            val sourceWidth = (LegacySaoHudMetrics.FOOD_BAR_SOURCE_WIDTH * foodRatio).roundToInt()
            if (foodWidth > 0 && sourceWidth > 0) {
                val rotten = data.activeEffects.any { it.id.namespace == "minecraft" && it.id.path == "hunger" }
                drawLegacyTexture(
                    element.texture,
                    LegacySaoHudMetrics.PLAYER_BAR_X + usernameWidth,
                    LegacySaoHudMetrics.FOOD_BAR_Y,
                    foodWidth,
                    LegacySaoHudMetrics.FOOD_BAR_HEIGHT,
                    0f,
                    LegacySaoHudMetrics.FOOD_BAR_V.toFloat(),
                    sourceWidth,
                    LegacySaoHudMetrics.FOOD_BAR_HEIGHT,
                    if (rotten) LegacySaoHudMetrics.FOOD_ROTTEN else LegacySaoHudMetrics.FOOD,
                    animatedContext,
                )
            }
        }

        val healthText = if (data.playerAbsorption > 0f) {
            Component.translatable(
                "formatHealthAbsorb",
                ceil(smoothHealth.toDouble()),
                ceil(data.playerMaxHealth.toDouble()),
                ceil(data.playerAbsorption.toDouble()),
            )
        } else {
            Component.translatable("formatHealth", ceil(smoothHealth.toDouble()), ceil(data.playerMaxHealth.toDouble()))
        }
        val healthTextWidth = font.width(healthText)
        val healthPanelX = usernameWidth + LegacySaoHudMetrics.HP_PANEL_BASE_X
        // hud.xml keeps the string before its translucent three-slice background.
        operations.text(
            healthText,
            healthPanelX + LegacySaoHudMetrics.VALUE_TEXT_X,
            LegacySaoHudMetrics.VALUE_TEXT_Y,
            renderColor(LegacySaoHudMetrics.WHITE, animatedContext),
            shadow = true,
            centered = false,
        )
        drawValuePanel(
            element.texture,
            healthPanelX,
            healthTextWidth,
            LegacySaoHudMetrics.HP_LEFT_U,
            LegacySaoHudMetrics.VALUE_CAP_WIDTH,
            LegacySaoHudMetrics.HP_MIDDLE_U,
            LegacySaoHudMetrics.HP_MIDDLE_SOURCE_WIDTH,
            LegacySaoHudMetrics.HP_RIGHT_U,
            LegacySaoHudMetrics.VALUE_CAP_WIDTH,
            animatedContext,
        )

        if (data.experienceVisible) {
            val levelText = Component.translatable("displayLvShort", data.experienceLevel)
            val levelWidth = font.width(levelText)
            // EXPERIENCE was a separate absolute HUD part; compensate for this element's (2,2) root.
            val levelX = usernameWidth + LegacySaoHudMetrics.LEVEL_PANEL_BASE_X + healthTextWidth
            // Like the HP group, hud.xml lists the string before the background slices.
            operations.text(
                levelText,
                levelX + LegacySaoHudMetrics.VALUE_TEXT_X,
                LegacySaoHudMetrics.VALUE_TEXT_Y,
                renderColor(LegacySaoHudMetrics.WHITE, animatedContext),
                shadow = true,
                centered = false,
            )
            drawValuePanel(
                element.texture,
                levelX,
                levelWidth,
                LegacySaoHudMetrics.LEVEL_LEFT_U,
                LegacySaoHudMetrics.LEVEL_LEFT_SOURCE_WIDTH,
                LegacySaoHudMetrics.LEVEL_MIDDLE_U,
                LegacySaoHudMetrics.LEVEL_MIDDLE_SOURCE_WIDTH,
                LegacySaoHudMetrics.LEVEL_RIGHT_U,
                LegacySaoHudMetrics.LEVEL_RIGHT_SOURCE_WIDTH,
                animatedContext,
            )
        }
    }

    override fun visit(element: LegacySaoEffectsElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        val startX = Minecraft.getInstance().font.width(data.playerName) +
            LegacySaoHudMetrics.USERNAME_WIDTH_PADDING + LegacySaoHudMetrics.EFFECTS_BASE_X
        // StatusEffects.getEffects() ignored effects that had no original SAO icon mapping.
        val textures = data.activeEffects.mapNotNull(::legacySaoEffectTexture).toMutableList()
        when {
            data.food <= 6 -> textures += legacySaoStateTexture("starving")
            data.food <= 18 -> textures += legacySaoStateTexture("hungry")
        }
        if (data.underwater && data.air < data.maxAir) {
            textures += legacySaoStateTexture(if (data.air <= 0) "drowning" else "wet")
        }
        if (data.onFire) textures += legacySaoStateTexture("burning")
        textures.forEachIndexed { index, texture ->
            operations.texture(
                texture,
                startX + index * LegacySaoHudMetrics.EFFECT_X_STEP,
                LegacySaoHudMetrics.EFFECTS_Y,
                LegacySaoHudMetrics.EFFECT_ICON_SIZE,
                LegacySaoHudMetrics.EFFECT_ICON_SIZE,
                0f,
                0f,
                LegacySaoHudMetrics.EFFECT_ICON_SIZE,
                LegacySaoHudMetrics.EFFECT_ICON_SIZE,
                LegacySaoHudMetrics.EFFECT_TEXTURE_SIZE,
                LegacySaoHudMetrics.EFFECT_TEXTURE_SIZE,
                renderColor(LegacySaoHudMetrics.WHITE, animatedContext),
            )
        }
    }

    override fun visit(element: LegacySaoEntityHealthElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        val font = Minecraft.getInstance().font
        // The active sao/hud.xml repeats nearbyEntitySize; targetEntity was populated
        // in the old context but is not referenced by this formal theme.
        val candidates = data.nearbyEntities.asSequence()
            .sortedBy { it.health / it.maxHealth }
            .take(LegacySaoHudMetrics.ENTITY_MAX_ROWS)
        candidates.forEachIndexed { index, entity ->
            val y = index * LegacySaoHudMetrics.ENTITY_ROW_HEIGHT
            operations.pushTransform()
            try {
                operations.scale(-1f, 1f)
                val ratio = (entity.health / entity.maxHealth).coerceIn(0f, 1f)
                val filled = (LegacySaoHudMetrics.ENTITY_FILL_MAX_WIDTH * ratio).roundToInt()
                if (filled > 0) {
                    operations.pushTransform()
                    try {
                        operations.translate(0f, LegacySaoHudMetrics.ENTITY_FILL_HALF_PIXEL_Y, 0f)
                        drawLegacyTexture(
                            element.texture,
                            LegacySaoHudMetrics.ENTITY_FILL_X,
                            y + LegacySaoHudMetrics.ENTITY_FILL_Y,
                            filled,
                            LegacySaoHudMetrics.ENTITY_FILL_HEIGHT,
                            LegacySaoHudMetrics.ENTITY_FILL_U.toFloat(),
                            LegacySaoHudMetrics.ENTITY_FILL_V.toFloat(),
                            LegacySaoHudMetrics.ENTITY_SOURCE_WIDTH,
                            LegacySaoHudMetrics.ENTITY_SOURCE_HEIGHT,
                            LegacySaoHudMetrics.HP_LOW,
                            animatedContext,
                        )
                    } finally {
                        operations.popTransform()
                    }
                }
                // The original XML draws the silver frame after the colored fill.
                drawLegacyTexture(
                    element.texture,
                    0,
                    y,
                    LegacySaoHudMetrics.ENTITY_FRAME_WIDTH,
                    LegacySaoHudMetrics.ENTITY_FRAME_HEIGHT,
                    LegacySaoHudMetrics.ENTITY_FRAME_U.toFloat(),
                    LegacySaoHudMetrics.ENTITY_FRAME_V.toFloat(),
                    LegacySaoHudMetrics.ENTITY_SOURCE_WIDTH,
                    LegacySaoHudMetrics.ENTITY_SOURCE_HEIGHT,
                    LegacySaoHudMetrics.WHITE,
                    animatedContext,
                )
            } finally {
                operations.popTransform()
            }
            val name = entity.displayName.take(LegacySaoHudMetrics.ENTITY_NAME_MAX_LENGTH)
            operations.text(
                name,
                -font.width(name) - LegacySaoHudMetrics.ENTITY_NAME_RIGHT_GAP,
                y + LegacySaoHudMetrics.ENTITY_NAME_Y,
                renderColor(LegacySaoHudMetrics.WHITE, animatedContext),
                shadow = true,
                centered = false,
            )
        }
    }

    override fun visit(element: EffectListElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        animations.effectVisuals(element.renderState.key, effectDisplays(element, data), element.entryAnimations)
            .asSequence().take(element.maxEffects).forEachIndexed { index, animated ->
                val display = animated.visual
                val runtime = animated.animation
                val offset = index * (element.rowHeight + element.spacing)
                val baseX = if (element.orientation == HotbarOrientation.HORIZONTAL) offset else 0
                val baseY = if (element.orientation == HotbarOrientation.VERTICAL) offset else 0
                val cellWidth = if (element.showLabels) element.width else maxOf(element.width, element.iconSize)
                val entryContext = animatedContext.copy(
                    alphaMultiplier = animatedContext.alphaMultiplier * runtime.alpha,
                    colorOverride = runtime.color ?: animatedContext.colorOverride,
                )
                operations.pushTransform()
                try {
                    operations.translate(baseX + runtime.translationX, baseY + runtime.translationY, 0f)
                    operations.scale(runtime.scale, runtime.scale)
                    element.backgroundColor?.let {
                        operations.fill(0, 0, cellWidth, element.rowHeight, renderColor(it, entryContext))
                    }
                    if (element.showLabels) {
                        val accent = if (display.beneficial) element.beneficialColor else element.harmfulColor
                        operations.fill(0, 0, EFFECT_ACCENT_WIDTH, element.rowHeight, renderColor(accent, entryContext))
                    }
                    val textX = if (element.showIcons) element.iconSize + EFFECT_ICON_GAP else EFFECT_TEXT_PADDING
                    if (element.showIcons) {
                        operations.texture(
                            texture = display.texture,
                            x = if (element.showLabels) EFFECT_ICON_X else 0,
                            y = (element.rowHeight - element.iconSize) / 2,
                            width = element.iconSize,
                            height = element.iconSize,
                            u = 0f,
                            v = 0f,
                            sourceWidth = element.iconSize,
                            sourceHeight = element.iconSize,
                            textureWidth = element.iconSize,
                            textureHeight = element.iconSize,
                            tint = renderColor(ArgbColor.WHITE, entryContext),
                        )
                    }
                    if (element.showLabels && display.effect != null) {
                        operations.text(
                            effectLabel(display.effect, element.showDuration),
                            textX,
                            (element.rowHeight - FONT_HEIGHT) / 2,
                            renderColor(element.textColor, entryContext),
                            shadow = true,
                            centered = false,
                        )
                    }
                } finally {
                    operations.popTransform()
                }
            }
    }

    override fun visit(element: EntityHealthListElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val data = hudData ?: return@withElement
        data.nearbyEntities.asSequence().take(element.maxEntities).forEachIndexed { index, entity ->
            val y = index * element.rowHeight
            drawTextureRegion(element.background, 0, y, element.width, element.rowHeight, animatedContext)
            val value = (entity.health / entity.maxHealth).coerceIn(0f, 1f)
            val filled = (element.width * value).roundToInt().coerceIn(0, element.width)
            if (filled > 0) {
                operations.enableScissor(ClipRect(0, y, filled, element.rowHeight))
                try {
                    drawTextureRegion(element.foreground, 0, y, element.width, element.rowHeight, animatedContext)
                } finally {
                    operations.disableScissor()
                }
            }
            operations.text(
                entity.displayName,
                element.width / 2,
                y + (element.rowHeight - FONT_HEIGHT) / 2,
                renderColor(element.textColor, animatedContext),
                shadow = true,
                centered = true,
            )
        }
    }

    override fun visit(element: TargetEntityHealthElement, context: RenderContext) = withElement(element, context) { animatedContext ->
        val target = animations.targetEntity(element.lingerMillis) ?: return@withElement
        drawTextureRegion(element.background, 0, 0, element.width, element.height, animatedContext)
        val ratio = (target.health / target.maxHealth).coerceIn(0f, 1f)
        val filled = (element.width * ratio).roundToInt().coerceIn(0, element.width)
        if (filled > 0) {
            operations.enableScissor(ClipRect(0, 0, filled, element.height))
            try {
                drawTextureRegion(element.foreground, 0, 0, element.width, element.height, animatedContext)
            } finally {
                operations.disableScissor()
            }
        }
        operations.text(
            target.displayName,
            element.width / 2,
            (element.height - FONT_HEIGHT) / 2,
            renderColor(element.textColor, animatedContext),
            shadow = true,
            centered = true,
        )
    }

    private fun drawHotbarBackground(element: HotbarElement, x: Int, y: Int, context: RenderContext) {
        element.slotTexture?.let {
            drawTextureRegion(it, x, y, element.slotSize, element.slotSize, context)
            return
        }
        element.slotBackgroundColor?.let {
            operations.fill(x, y, element.slotSize, element.slotSize, renderColor(it, context))
        }
    }

    private fun drawHotbarSelection(element: HotbarElement, x: Int, y: Int, context: RenderContext) {
        element.selectedSlotTexture?.let {
            drawTextureRegion(it, x, y, element.slotSize, element.slotSize, context)
            return
        }
        element.selectedSlotColor?.let { selected ->
            operations.fill(x, y, element.slotSize, element.slotSize, renderColor(selected, context))
            element.slotBackgroundColor?.takeIf { element.slotSize > 2 }?.let { background ->
                operations.fill(x + 1, y + 1, element.slotSize - 2, element.slotSize - 2, renderColor(background, context))
            }
        }
    }

    private fun drawHotbarItem(
        element: HotbarElement,
        stack: ItemStack,
        x: Int,
        y: Int,
        popTime: Int,
        partialTick: Float,
    ) {
        val itemX = x + element.itemXOffset
        val itemY = y + element.itemYOffset
        val remainingPop = popTime - partialTick
        if (remainingPop <= 0f) {
            operations.item(stack, itemX, itemY, element.decorations)
            return
        }
        val popScale = 1f + remainingPop / 5f
        operations.pushTransform()
        try {
            operations.translate(itemX + 8f, itemY + 12f, 0f)
            operations.scale(1f / popScale, (popScale + 1f) / 2f)
            operations.translate(-(itemX + 8f), -(itemY + 12f), 0f)
            operations.item(stack, itemX, itemY, element.decorations)
        } finally {
            operations.popTransform()
        }
    }

    private fun withElement(element: Element, context: RenderContext, draw: (RenderContext) -> Unit) {
        if (!element.renderState.enabled) return

        val runtime = animations.elementRuntime(
            element.renderState.key,
            element.renderState.animations,
            context.visibilityTarget,
        )
        if (runtime.alpha <= 0f || runtime.scale <= 0f) return

        operations.pushTransform()
        try {
            val transform = element.transform
            val anchorX = when (transform.anchor) {
                HudAnchor.TOP_LEFT, HudAnchor.CENTER_LEFT, HudAnchor.BOTTOM_LEFT -> 0f
                HudAnchor.TOP_CENTER, HudAnchor.CENTER, HudAnchor.BOTTOM_CENTER -> context.guiWidth / 2f
                HudAnchor.TOP_RIGHT, HudAnchor.CENTER_RIGHT, HudAnchor.BOTTOM_RIGHT -> context.guiWidth.toFloat()
            }
            val anchorY = when (transform.anchor) {
                HudAnchor.TOP_LEFT, HudAnchor.TOP_CENTER, HudAnchor.TOP_RIGHT -> 0f
                HudAnchor.CENTER_LEFT, HudAnchor.CENTER, HudAnchor.CENTER_RIGHT -> context.guiHeight / 2f
                HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_CENTER, HudAnchor.BOTTOM_RIGHT -> context.guiHeight.toFloat()
            }
            operations.translate(
                anchorX + transform.x + runtime.translationX,
                anchorY + transform.y + runtime.translationY,
                transform.z,
            )
            operations.scale(transform.scaleX * runtime.scale, transform.scaleY * runtime.scale)
            val animatedContext = context.copy(
                alphaMultiplier = context.alphaMultiplier * runtime.alpha,
                colorOverride = runtime.color ?: context.colorOverride,
            )

            val clip = element.renderState.clip
            if (clip == null) {
                draw(animatedContext)
            } else {
                operations.enableScissor(clip)
                try {
                    draw(animatedContext)
                } finally {
                    operations.disableScissor()
                }
            }
        } finally {
            operations.popTransform()
        }
    }

    private fun drawTextureRegion(
        region: TextureRegion,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        context: RenderContext,
    ) {
        operations.texture(
            texture = region.texture,
            x = x,
            y = y,
            width = width,
            height = height,
            u = region.u,
            v = region.v,
            sourceWidth = region.sourceWidth,
            sourceHeight = region.sourceHeight,
            textureWidth = region.textureWidth,
            textureHeight = region.textureHeight,
            tint = renderColor(region.tint, context),
        )
    }

    /** Three-slice HP/LV backgrounds, preserving the child order and UVs in sao/hud.xml. */
    private fun drawValuePanel(
        texture: ResourceLocation,
        x: Int,
        middleWidth: Int,
        leftU: Int,
        leftSourceWidth: Int,
        middleU: Int,
        middleSourceWidth: Int,
        rightU: Int,
        rightSourceWidth: Int,
        context: RenderContext,
    ) {
        drawLegacyTexture(
            texture,
            x,
            LegacySaoHudMetrics.VALUE_PANEL_Y,
            LegacySaoHudMetrics.VALUE_CAP_WIDTH,
            LegacySaoHudMetrics.VALUE_PANEL_HEIGHT,
            leftU.toFloat(),
            LegacySaoHudMetrics.VALUE_PANEL_V.toFloat(),
            leftSourceWidth,
            LegacySaoHudMetrics.VALUE_PANEL_HEIGHT,
            LegacySaoHudMetrics.WHITE,
            context,
        )
        drawLegacyTexture(
            texture,
            x + LegacySaoHudMetrics.VALUE_CAP_WIDTH,
            LegacySaoHudMetrics.VALUE_PANEL_Y,
            middleWidth,
            LegacySaoHudMetrics.VALUE_PANEL_HEIGHT,
            middleU.toFloat(),
            LegacySaoHudMetrics.VALUE_PANEL_V.toFloat(),
            middleSourceWidth,
            LegacySaoHudMetrics.VALUE_PANEL_HEIGHT,
            LegacySaoHudMetrics.WHITE,
            context,
        )
        drawLegacyTexture(
            texture,
            x + LegacySaoHudMetrics.VALUE_CAP_WIDTH + middleWidth,
            LegacySaoHudMetrics.VALUE_PANEL_Y,
            LegacySaoHudMetrics.VALUE_CAP_WIDTH,
            LegacySaoHudMetrics.VALUE_PANEL_HEIGHT,
            rightU.toFloat(),
            LegacySaoHudMetrics.VALUE_PANEL_V.toFloat(),
            rightSourceWidth,
            LegacySaoHudMetrics.VALUE_PANEL_HEIGHT,
            LegacySaoHudMetrics.WHITE,
            context,
        )
    }

    private fun drawLegacyTexture(
        texture: ResourceLocation,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        u: Float,
        v: Float,
        sourceWidth: Int,
        sourceHeight: Int,
        tint: ArgbColor,
        context: RenderContext,
    ) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) return
        operations.texture(
            texture = texture,
            x = x,
            y = y,
            width = width,
            height = height,
            u = u,
            v = v,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            textureWidth = LegacySaoHudMetrics.ATLAS_SIZE,
            textureHeight = LegacySaoHudMetrics.ATLAS_SIZE,
            tint = renderColor(tint, context),
        )
    }

    private fun drawCroppedTextureRegion(
        region: TextureRegion,
        width: Int,
        height: Int,
        crop: ClipRect,
        context: RenderContext,
    ) {
        if (crop.width <= 0 || crop.height <= 0) return
        val uOffset = region.sourceWidth * (crop.x.toFloat() / width)
        val vOffset = region.sourceHeight * (crop.y.toFloat() / height)
        val sourceWidth = (region.sourceWidth * (crop.width.toFloat() / width)).roundToInt().coerceAtLeast(1)
        val sourceHeight = (region.sourceHeight * (crop.height.toFloat() / height)).roundToInt().coerceAtLeast(1)
        operations.texture(
            texture = region.texture,
            x = crop.x,
            y = crop.y,
            width = crop.width,
            height = crop.height,
            u = region.u + uOffset,
            v = region.v + vOffset,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            textureWidth = region.textureWidth,
            textureHeight = region.textureHeight,
            tint = renderColor(region.tint, context),
        )
    }

    private fun drawProgressTexture(
        region: TextureRegion,
        element: TexturedProgressBarElement,
        value: Float,
        context: RenderContext,
    ) {
        if (value <= 0f) return
        val filledWidth = (element.width * value).roundToInt().coerceIn(0, element.width)
        val filledHeight = (element.height * value).roundToInt().coerceIn(0, element.height)
        val clipRect = when (element.direction) {
            ProgressDirection.LEFT_TO_RIGHT -> ClipRect(0, 0, filledWidth, element.height)
            ProgressDirection.RIGHT_TO_LEFT -> ClipRect(element.width - filledWidth, 0, filledWidth, element.height)
            ProgressDirection.TOP_TO_BOTTOM -> ClipRect(0, 0, element.width, filledHeight)
            ProgressDirection.BOTTOM_TO_TOP -> ClipRect(0, element.height - filledHeight, element.width, filledHeight)
        }
        if (element.clip) {
            operations.enableScissor(clipRect)
            try {
                drawTextureRegion(region, 0, 0, element.width, element.height, context)
            } finally {
                operations.disableScissor()
            }
        } else {
            drawCroppedTextureRegion(region, element.width, element.height, clipRect, context)
        }
    }

    private fun renderColor(color: ArgbColor, context: RenderContext): ArgbColor =
        (context.colorOverride ?: color).multiplyAlpha(context.alphaMultiplier)

    private fun effectDisplays(element: EffectListElement, data: HudDataSnapshot): List<EffectVisualSnapshot> {
        val displays = data.activeEffects.asSequence()
            .filter(HudEffectSnapshot::showIcon)
            .map { effect ->
                EffectVisualSnapshot(
                    stableKey = "effect:${effect.id}#${effect.amplifier}",
                    effect = effect,
                    texture = when (element.iconSet) {
                        HudEffectIconSet.VANILLA -> vanillaEffectTexture(effect)
                        HudEffectIconSet.LEGACY_SAO -> legacySaoEffectTexture(effect) ?: vanillaEffectTexture(effect)
                    },
                    beneficial = effect.beneficial,
                )
            }
            .toMutableList()

        if (element.iconSet == HudEffectIconSet.LEGACY_SAO && element.includePlayerStates) {
            when {
                data.food <= 6 -> displays += legacySaoState("starving", beneficial = false)
                data.food <= 18 -> displays += legacySaoState("hungry", beneficial = false)
            }
            if (data.underwater && data.air < data.maxAir) {
                displays += legacySaoState(if (data.air <= 0) "drowning" else "wet", beneficial = false)
            }
            if (data.onFire) displays += legacySaoState("burning", beneficial = false)
        }
        return displays
    }

    private fun vanillaEffectTexture(effect: HudEffectSnapshot): ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        effect.id.namespace,
        "textures/mob_effect/${effect.id.path}.png",
    )

    private fun legacySaoEffectTexture(effect: HudEffectSnapshot): ResourceLocation? {
        if (effect.id.namespace != "minecraft") return null
        val icon = when (effect.id.path) {
            "slowness" -> if (effect.amplifier > 5) "paralyzed" else "slowness"
            "poison" -> "poisoned"
            "hunger" -> "rotten"
            "nausea" -> "ill"
            "weakness" -> "weak"
            "wither" -> "cursed"
            "blindness" -> "blind"
            "saturation" -> "saturation"
            "speed" -> "speed_boost"
            "water_breathing" -> "water_breath"
            "strength" -> "strength"
            "absorption" -> "absorption"
            "fire_resistance" -> "fire_res"
            "haste" -> "haste"
            "health_boost" -> "health_boost"
            "instant_health" -> "inst_health"
            "invisibility" -> "invisibility"
            "jump_boost" -> "jump_boost"
            "night_vision" -> "night_vision"
            "regeneration" -> "regen"
            "resistance" -> "resist"
            else -> return null
        }
        return legacySaoStateTexture(icon)
    }

    private fun legacySaoState(icon: String, beneficial: Boolean): EffectVisualSnapshot = EffectVisualSnapshot(
        stableKey = "state:$icon",
        effect = null,
        texture = legacySaoStateTexture(icon),
        beneficial = beneficial,
    )

    private fun legacySaoStateTexture(icon: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(
        "saoui",
        "textures/sao/status_icons/$icon.png",
    )

    private fun effectLabel(effect: HudEffectSnapshot, showDuration: Boolean): String {
        val name = Component.translatable("effect.${effect.id.namespace}.${effect.id.path}").string
        val level = if (effect.amplifier <= 0) "" else " ${effect.amplifier + 1}"
        val duration = if (showDuration) "  ${formatDuration(effect.durationTicks)}" else ""
        return "$name$level$duration"
    }

    private fun formatDuration(durationTicks: Int): String {
        if (durationTicks < 0) return "∞"
        val seconds = ceil(durationTicks / TICKS_PER_SECOND).toInt()
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }

    private companion object {
        const val EFFECT_ACCENT_WIDTH = 2
        const val EFFECT_ICON_X = 4
        const val EFFECT_ICON_GAP = 8
        const val EFFECT_TEXT_PADDING = 5
        const val FONT_HEIGHT = 9
        const val TICKS_PER_SECOND = 20.0
    }

}
