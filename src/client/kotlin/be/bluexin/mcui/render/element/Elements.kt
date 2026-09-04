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
import be.bluexin.mcui.animation.ResolvedAnimationSpec
import be.bluexin.mcui.animation.ResolvedHealthAnimationSpec
import be.bluexin.mcui.themes.HudEffectIconSet
import be.bluexin.mcui.themes.HudItemSource
import be.bluexin.mcui.themes.HotbarOrientation
import be.bluexin.mcui.themes.HudTextSource
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
    fun visit(element: TexturedProgressBarElement, context: RenderContext)
    fun visit(element: DynamicTextElement, context: RenderContext)
    fun visit(element: HudItemElement, context: RenderContext)
    fun visit(element: HotbarElement, context: RenderContext)
    fun visit(element: EffectListElement, context: RenderContext)
    fun visit(element: EntityHealthListElement, context: RenderContext)
    fun visit(element: TargetEntityHealthElement, context: RenderContext)
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

data class TextureRegion(
    val texture: ResourceLocation,
    val u: Float,
    val v: Float,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val textureWidth: Int,
    val textureHeight: Int,
    val tint: ArgbColor = ArgbColor.WHITE,
)

data class ProgressTint(
    val maximum: Float,
    val tint: ArgbColor,
)

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

/** A HUD value rendered by clipping or cropping one immutable texture region. */
data class TexturedProgressBarElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val width: Int,
    val height: Int,
    val background: TextureRegion?,
    val foreground: TextureRegion,
    val direction: ProgressDirection,
    val valueSource: HudValueSource,
    val clip: Boolean,
    val valueTints: List<ProgressTint> = emptyList(),
    val creativeTint: ArgbColor? = null,
    val delayedForeground: TextureRegion? = null,
    val healthAnimation: ResolvedHealthAnimationSpec? = null,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class DynamicTextElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val valueSource: HudValueSource? = null,
    val textSource: HudTextSource? = null,
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
    val slotTexture: TextureRegion?,
    val selectedSlotTexture: TextureRegion?,
    val orientation: HotbarOrientation,
    val decorations: Boolean,
    val showOffhand: Boolean = false,
    val offhandGap: Int = 0,
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
    val showLabels: Boolean = true,
    val orientation: HotbarOrientation = HotbarOrientation.VERTICAL,
    val spacing: Int = 0,
    val iconSize: Int = 18,
    val iconSet: HudEffectIconSet = HudEffectIconSet.VANILLA,
    val includePlayerStates: Boolean = false,
    val entryAnimations: List<ResolvedAnimationSpec> = emptyList(),
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

data class EntityHealthListElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val width: Int,
    val rowHeight: Int,
    val maxEntities: Int,
    val background: TextureRegion,
    val foreground: TextureRegion,
    val textColor: ArgbColor,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}

/** Optional legacy target card; the bundled SAO theme continues to use its historical nearby list. */
data class TargetEntityHealthElement(
    override val renderState: ResolvedRenderState = ResolvedRenderState(),
    override val transform: ResolvedTransform = ResolvedTransform.IDENTITY,
    val width: Int,
    val height: Int,
    val background: TextureRegion,
    val foreground: TextureRegion,
    val textColor: ArgbColor,
    val lingerMillis: Int = 3000,
) : Element {
    override fun accept(visitor: ElementVisitor, context: RenderContext) = visitor.visit(this, context)
}
