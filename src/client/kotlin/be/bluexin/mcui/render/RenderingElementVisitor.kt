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
import be.bluexin.mcui.render.element.DynamicTextElement
import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.ElementVisitor
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.HotbarElement
import be.bluexin.mcui.render.element.HudItemElement
import be.bluexin.mcui.render.element.ItemElement
import be.bluexin.mcui.render.element.ProgressBarElement
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import be.bluexin.mcui.render.element.TextureElement
import be.bluexin.mcui.themes.HudAnchor
import be.bluexin.mcui.themes.ProgressDirection
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

    override fun visit(element: DynamicTextElement, context: RenderContext) = withElement(element, context) {
        val data = hudData ?: return@withElement
        operations.text(
            HudBindingResolver.text(element.valueSource, data),
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
            val x = index * (element.slotSize + element.slotSpacing)
            val selected = index == data.selectedHotbarSlot
            val selectedColor = element.selectedSlotColor
            val background = element.slotBackgroundColor

            if (selected && selectedColor != null) {
                operations.fill(x, 0, element.slotSize, element.slotSize, selectedColor)
                if (background != null && element.slotSize > 2) {
                    operations.fill(x + 1, 1, element.slotSize - 2, element.slotSize - 2, background)
                }
            } else if (background != null) {
                operations.fill(x, 0, element.slotSize, element.slotSize, background)
            }

            operations.item(
                stack,
                x + element.itemXOffset,
                element.itemYOffset,
                element.decorations,
            )
        }
    }

    private fun withElement(element: Element, context: RenderContext, draw: () -> Unit) {
        if (!element.renderState.enabled) return

        operations.pushTransform()
        try {
            val transform = element.transform
            val anchorX = when (transform.anchor) {
                HudAnchor.TOP_LEFT, HudAnchor.BOTTOM_LEFT -> 0f
                HudAnchor.TOP_CENTER, HudAnchor.CENTER, HudAnchor.BOTTOM_CENTER -> context.guiWidth / 2f
                HudAnchor.TOP_RIGHT, HudAnchor.BOTTOM_RIGHT -> context.guiWidth.toFloat()
            }
            val anchorY = when (transform.anchor) {
                HudAnchor.TOP_LEFT, HudAnchor.TOP_CENTER, HudAnchor.TOP_RIGHT -> 0f
                HudAnchor.CENTER -> context.guiHeight / 2f
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
}
