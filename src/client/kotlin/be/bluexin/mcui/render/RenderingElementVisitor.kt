/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.ElementVisitor
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.ItemElement
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import be.bluexin.mcui.render.element.TextureElement

/** Traverses resolved elements and is the only layer allowed to issue render operations. */
class RenderingElementVisitor(
    private val operations: GuiRenderOperations,
) : ElementVisitor {
    fun render(root: Element, context: RenderContext) {
        root.accept(this, context)
    }

    override fun visit(element: GroupElement, context: RenderContext) = withElement(element) {
        element.children.sortedBy { it.transform.z }.forEach { it.accept(this, context) }
    }

    override fun visit(element: RectangleElement, context: RenderContext) = withElement(element) {
        operations.fill(0, 0, element.width, element.height, element.color)
    }

    override fun visit(element: TextElement, context: RenderContext) = withElement(element) {
        operations.text(element.text, 0, 0, element.color, element.shadow, element.centered)
    }

    override fun visit(element: TextureElement, context: RenderContext) = withElement(element) {
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

    override fun visit(element: ItemElement, context: RenderContext) = withElement(element) {
        operations.item(element.stack, 0, 0, element.decorations, element.countText)
    }

    private fun withElement(element: Element, draw: () -> Unit) {
        if (!element.renderState.enabled) return

        operations.pushTransform()
        try {
            val transform = element.transform
            operations.translate(transform.x, transform.y, transform.z)
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
