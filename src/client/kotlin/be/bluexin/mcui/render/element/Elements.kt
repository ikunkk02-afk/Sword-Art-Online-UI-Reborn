/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render.element

import be.bluexin.mcui.render.ArgbColor
import be.bluexin.mcui.render.RenderContext
import be.bluexin.mcui.render.ResolvedRenderState
import be.bluexin.mcui.render.ResolvedTransform
import be.bluexin.mcui.themes.HudItemSource
import be.bluexin.mcui.themes.HudValueSource
import be.bluexin.mcui.themes.ProgressDirection
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

interface Element {
    val renderState: ResolvedRenderState
    val transform: ResolvedTransform
    fun accept(visitor: ElementVisitor, context: RenderContext)
}

interface ElementVisitor {
    fun visit(element: GroupElement, context: RenderContext)
    fun visit(element: RectangleElement, context: RenderContext)
    fun visit(element: TextElement, context: RenderContext)
    fun visit(element: TextureElement, context: RenderContext)
    fun visit(element: ItemElement, context: RenderContext)
    fun visit(element: ProgressBarElement, context: RenderContext)
    fun visit(element: DynamicTextElement, context: RenderContext)
    fun visit(element: HudItemElement, context: RenderContext)
    fun visit(element: HotbarElement, context: RenderContext)
    fun visit(element: EffectListElement, context: RenderContext)
}

data class GroupElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val children: List<Element>,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class RectangleElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val width: Int,
    val height: Int,
    val color: ArgbColor,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class TextElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val text: Component,
    val color: ArgbColor = ArgbColor.WHITE,
    val shadow: Boolean = false,
    val centered: Boolean = false,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class TextureElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val texture: ResourceLocation,
    val width: Int,
    val height: Int,
    val u: Float = 0f,
    val v: Float = 0f,
    val sourceWidth: Int = width,
    val sourceHeight: Int = height,
    val textureWidth: Int = 256,
    val textureHeight: Int = 256,
    val tint: ArgbColor = ArgbColor.WHITE,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class ItemElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val stack: ItemStack,
    val decorations: Boolean = true,
    val countText: String? = null,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class ProgressBarElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val width: Int,
    val height: Int,
    val backgroundColor: ArgbColor?,
    val foregroundColor: ArgbColor,
    val direction: ProgressDirection,
    val valueSource: HudValueSource,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class DynamicTextElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val valueSource: HudValueSource,
    val color: ArgbColor = ArgbColor.WHITE,
    val shadow: Boolean = false,
    val centered: Boolean = false,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class HudItemElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val source: HudItemSource,
    val decorations: Boolean = true,
    val countText: String? = null,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class HotbarElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val slotSize: Int,
    val slotSpacing: Int,
    val itemXOffset: Int,
    val itemYOffset: Int,
    val slotBackgroundColor: ArgbColor?,
    val selectedSlotColor: ArgbColor?,
    val decorations: Boolean,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class EffectListElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val width: Int,
    val rowHeight: Int,
    val maxEffects: Int,
    val backgroundColor: ArgbColor?,
    val textColor: ArgbColor,
    val beneficialColor: ArgbColor,
    val harmfulColor: ArgbColor,
    val showDuration: Boolean,
    val showIcons: Boolean,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}
