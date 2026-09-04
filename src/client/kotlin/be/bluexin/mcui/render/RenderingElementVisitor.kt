/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

import be.bluexin.mcui.fabric.client.hud.HudBindingResolver
import be.bluexin.mcui.fabric.client.hud.HudDataSnapshot
import be.bluexin.mcui.fabric.client.hud.HudEffectSnapshot
import be.bluexin.mcui.render.element.DynamicTextElement
import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.EffectListElement
import be.bluexin.mcui.render.element.ElementVisitor
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.HotbarElement
import be.bluexin.mcui.render.element.HudItemElement
import be.bluexin.mcui.render.element.ItemElement
import be.bluexin.mcui.render.element.ProgressBarElement
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import be.bluexin.mcui.render.element.TexturedProgressBarElement
import be.bluexin.mcui.render.element.TextureElement
import be.bluexin.mcui.render.element.TextureRegion
import be.bluexin.mcui.themes.HudAnchor
import be.bluexin.mcui.themes.HudEffectIconSet
import be.bluexin.mcui.themes.HotbarOrientation
import be.bluexin.mcui.themes.ProgressDirection
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Traverses resolved elements and is the only layer allowed to issue render operations. */
class RenderingElementVisitor(
    private val operations: GuiRenderOperations,
    private val hudData: HudDataSnapshot? = null,
) : ElementVisitor {
    fun render(root: Element, context: RenderContext) {
        root.accept(this, context)
    }

    override fun visit(element: GroupElement, context: RenderContext) = withElement(element, context) {
        element.children.sortedBy { it.transform.z }.forEach { it.accept(this, context) }
    }

    override fun visit(element: RectangleElement, context: RenderContext) = withElement(element, context) {
        operations.fill(0, 0, element.width, element.height, element.color)
    }

    override fun visit(element: TextElement, context: RenderContext) = withElement(element, context) {
        operations.text(element.text, 0, 0, element.color, element.shadow, element.centered)
    }

    override fun visit(element: TextureElement, context: RenderContext) = withElement(element, context) {
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
            tint = element.tint,
        )
    }

    override fun visit(element: ItemElement, context: RenderContext) = withElement(element, context) {
        operations.item(element.stack, 0, 0, element.decorations, element.countText)
    }

    override fun visit(element: ProgressBarElement, context: RenderContext) = withElement(element, context) {
        val data = hudData ?: return@withElement
        element.backgroundColor?.let { operations.fill(0, 0, element.width, element.height, it) }
        val value = HudBindingResolver.progress(element.valueSource, data)
        when (element.direction) {
            ProgressDirection.LEFT_TO_RIGHT -> {
                val filled = (element.width * value).roundToInt()
                operations.fill(0, 0, filled, element.height, element.foregroundColor)
            }

            ProgressDirection.RIGHT_TO_LEFT -> {
                val filled = (element.width * value).roundToInt()
                operations.fill(element.width - filled, 0, filled, element.height, element.foregroundColor)
            }

            ProgressDirection.TOP_TO_BOTTOM -> {
                val filled = (element.height * value).roundToInt()
                operations.fill(0, 0, element.width, filled, element.foregroundColor)
            }

            ProgressDirection.BOTTOM_TO_TOP -> {
                val filled = (element.height * value).roundToInt()
                operations.fill(0, element.height - filled, element.width, filled, element.foregroundColor)
            }
        }
    }

    override fun visit(element: TexturedProgressBarElement, context: RenderContext) = withElement(element, context) {
        val data = hudData ?: return@withElement
        element.background?.let { drawTextureRegion(it, 0, 0, element.width, element.height) }
        val value = HudBindingResolver.progress(element.valueSource, data)
        if (value <= 0f) return@withElement
        val dynamicTint = when {
            data.creative && element.creativeTint != null -> element.creativeTint
            else -> element.valueTints.firstOrNull { value <= it.maximum }?.tint
        }
        val foreground = dynamicTint?.let { element.foreground.copy(tint = it) } ?: element.foreground

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
                drawTextureRegion(foreground, 0, 0, element.width, element.height)
            } finally {
                operations.disableScissor()
            }
        } else {
            drawCroppedTextureRegion(foreground, element.width, element.height, clipRect)
        }
    }

    override fun visit(element: DynamicTextElement, context: RenderContext) = withElement(element, context) {
        val data = hudData ?: return@withElement
        operations.text(
            element.textSource?.let { HudBindingResolver.text(it, data) }
                ?: element.valueSource?.let { HudBindingResolver.text(it, data) }
                ?: return@withElement,
            0,
            0,
            element.color,
            element.shadow,
            element.centered,
        )
    }

    override fun visit(element: HudItemElement, context: RenderContext) = withElement(element, context) {
        val data = hudData ?: return@withElement
        operations.item(
            HudBindingResolver.item(element.source, data),
            0,
            0,
            element.decorations,
            element.countText,
        )
    }

    override fun visit(element: HotbarElement, context: RenderContext) = withElement(element, context) {
        val data = hudData ?: return@withElement
        data.hotbarItems.forEachIndexed { index, stack ->
            val offset = index * (element.slotSize + element.slotSpacing)
            val x = if (element.orientation == HotbarOrientation.HORIZONTAL) offset else 0
            val y = if (element.orientation == HotbarOrientation.VERTICAL) offset else 0
            drawHotbarSlot(element, stack, x, y, index == data.selectedHotbarSlot)
        }

        if (element.showOffhand && !data.offHandItem.isEmpty) {
            val offset = data.hotbarItems.size * (element.slotSize + element.slotSpacing) + element.offhandGap
            val x = if (element.orientation == HotbarOrientation.HORIZONTAL) offset else 0
            val y = if (element.orientation == HotbarOrientation.VERTICAL) offset else 0
            drawHotbarSlot(element, data.offHandItem, x, y, selected = true)
        }
    }

    override fun visit(element: EffectListElement, context: RenderContext) = withElement(element, context) {
        val data = hudData ?: return@withElement
        effectDisplays(element, data).asSequence()
            .take(element.maxEffects)
            .forEachIndexed { index, display ->
                val offset = index * (element.rowHeight + element.spacing)
                val x = if (element.orientation == HotbarOrientation.HORIZONTAL) offset else 0
                val y = if (element.orientation == HotbarOrientation.VERTICAL) offset else 0
                val cellWidth = if (element.showLabels) element.width else maxOf(element.width, element.iconSize)
                element.backgroundColor?.let { operations.fill(x, y, cellWidth, element.rowHeight, it) }
                if (element.showLabels) {
                    val accent = if (display.beneficial) element.beneficialColor else element.harmfulColor
                    operations.fill(x, y, EFFECT_ACCENT_WIDTH, element.rowHeight, accent)
                }

                val textX = x + if (element.showIcons) element.iconSize + EFFECT_ICON_GAP else EFFECT_TEXT_PADDING
                if (element.showIcons) {
                    operations.texture(
                        texture = display.texture,
                        x = x + if (element.showLabels) EFFECT_ICON_X else 0,
                        y = y + (element.rowHeight - element.iconSize) / 2,
                        width = element.iconSize,
                        height = element.iconSize,
                        u = 0f,
                        v = 0f,
                        sourceWidth = element.iconSize,
                        sourceHeight = element.iconSize,
                        textureWidth = element.iconSize,
                        textureHeight = element.iconSize,
                        tint = ArgbColor.WHITE,
                    )
                }
                if (element.showLabels && display.effect != null) {
                    operations.text(
                        effectLabel(display.effect, element.showDuration),
                        textX,
                        y + (element.rowHeight - FONT_HEIGHT) / 2,
                        element.textColor,
                        shadow = true,
                        centered = false,
                    )
                }
            }
    }

    private fun drawHotbarSlot(element: HotbarElement, stack: ItemStack, x: Int, y: Int, selected: Boolean) {
        val selectedColor = element.selectedSlotColor
        val background = element.slotBackgroundColor
        val themedSlot = if (selected) element.selectedSlotTexture ?: element.slotTexture else element.slotTexture
        if (themedSlot != null) {
            drawTextureRegion(themedSlot, x, y, element.slotSize, element.slotSize)
        } else if (selected && selectedColor != null) {
            operations.fill(x, y, element.slotSize, element.slotSize, selectedColor)
            if (background != null && element.slotSize > 2) {
                operations.fill(x + 1, y + 1, element.slotSize - 2, element.slotSize - 2, background)
            }
        } else if (background != null) {
            operations.fill(x, y, element.slotSize, element.slotSize, background)
        }
        operations.item(stack, x + element.itemXOffset, y + element.itemYOffset, element.decorations)
    }

    private fun withElement(element: Element, context: RenderContext, draw: () -> Unit) {
        if (!element.renderState.enabled) return

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
            operations.translate(anchorX + transform.x, anchorY + transform.y, transform.z)
            operations.scale(transform.scaleX, transform.scaleY)

            val clip = element.renderState.clip
            if (clip == null) {
                draw()
            } else {
                operations.enableScissor(clip)
                try {
                    draw()
                } finally {
                    operations.disableScissor()
                }
            }
        } finally {
            operations.popTransform()
        }
    }

    private fun drawTextureRegion(region: TextureRegion, x: Int, y: Int, width: Int, height: Int) {
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
            tint = region.tint,
        )
    }

    private fun drawCroppedTextureRegion(region: TextureRegion, width: Int, height: Int, crop: ClipRect) {
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
            tint = region.tint,
        )
    }

    private fun effectDisplays(element: EffectListElement, data: HudDataSnapshot): List<EffectDisplay> {
        val displays = data.activeEffects.asSequence()
            .filter(HudEffectSnapshot::showIcon)
            .map { effect ->
                EffectDisplay(
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

    private fun legacySaoState(icon: String, beneficial: Boolean): EffectDisplay = EffectDisplay(
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

    private data class EffectDisplay(
        val effect: HudEffectSnapshot?,
        val texture: ResourceLocation,
        val beneficial: Boolean,
    )
}
