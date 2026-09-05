/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

import be.bluexin.mcui.Constants
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/** Minecraft 1.21.1 Mojang-mapped adapter backed by GuiGraphics. */
class MinecraftGuiRenderOperations(
    private val graphics: GuiGraphics,
    private val minecraft: Minecraft = Minecraft.getInstance(),
) : GuiRenderOperations {
    private var transformDepth = 0
    private val scissors = ScissorStack(
        enableNative = { graphics.enableScissor(it.left, it.top, it.right, it.bottom) },
        disableNative = graphics::disableScissor,
    )

    override fun pushTransform() {
        graphics.pose().pushPose()
        transformDepth++
    }

    override fun popTransform() {
        check(transformDepth > 0) { "MCUI transform stack underflow" }
        graphics.pose().popPose()
        transformDepth--
    }

    override fun translate(x: Float, y: Float, z: Float) {
        // Preserve the original half-pixel entity fill offset and animated transforms.
        // Rounding here silently erased explicit offsets supplied by the renderer.
        graphics.pose().translate(x, y, z)
    }

    override fun scale(x: Float, y: Float) {
        graphics.pose().scale(x, y, 1f)
    }

    override fun fill(x: Int, y: Int, width: Int, height: Int, color: ArgbColor) {
        if (width <= 0 || height <= 0 || color.alpha <= 0f) return
        graphics.fill(x, y, x + width, y + height, color.value)
    }

    override fun texture(
        texture: ResourceLocation,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        u: Float,
        v: Float,
        sourceWidth: Int,
        sourceHeight: Int,
        textureWidth: Int,
        textureHeight: Int,
        tint: ArgbColor,
    ) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0 || tint.alpha <= 0f) return
        require(textureWidth > 0 && textureHeight > 0) { "Texture dimensions must be positive" }

        // GuiGraphics has no tinted ResourceLocation blit overload in 1.21.1.
        // Preserve both pieces of global state touched by the canonical vanilla pattern.
        val previousColor = RenderSystem.getShaderColor().copyOf()
        val blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND)
        // The enemy bar uses negative X scaling, reversing quad winding.
        // Legacy renderEnemyHealth explicitly disabled culling for these quads.
        val cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE)
        if (cullWasEnabled) RenderSystem.disableCull()
        if (!blendWasEnabled) RenderSystem.enableBlend()
        try {
            graphics.setColor(tint.red, tint.green, tint.blue, tint.alpha)
            graphics.blit(
                texture,
                x,
                y,
                width,
                height,
                u,
                v,
                sourceWidth,
                sourceHeight,
                textureWidth,
                textureHeight,
            )
        } finally {
            graphics.setColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3])
            if (!blendWasEnabled) RenderSystem.disableBlend()
            if (cullWasEnabled) RenderSystem.enableCull()
        }
    }

    override fun text(
        text: Component,
        x: Int,
        y: Int,
        color: ArgbColor,
        shadow: Boolean,
        centered: Boolean,
    ) {
        val drawX = if (centered) x - minecraft.font.width(text) / 2 else x
        graphics.drawString(minecraft.font, text, drawX, y, color.value, shadow)
    }

    override fun text(
        text: String,
        x: Int,
        y: Int,
        color: ArgbColor,
        shadow: Boolean,
        centered: Boolean,
    ) {
        val drawX = if (centered) x - minecraft.font.width(text) / 2 else x
        graphics.drawString(minecraft.font, text, drawX, y, color.value, shadow)
    }

    override fun item(stack: ItemStack, x: Int, y: Int, decorations: Boolean, countText: String?) {
        if (stack.isEmpty) return
        graphics.renderItem(stack, x, y)
        if (decorations) graphics.renderItemDecorations(minecraft.font, stack, x, y, countText)
    }

    override fun enableScissor(rect: ClipRect) {
        val matrix = graphics.pose().last().pose()
        val corners = arrayOf(
            Vector3f(rect.x.toFloat(), rect.y.toFloat(), 0f),
            Vector3f((rect.x + rect.width).toFloat(), rect.y.toFloat(), 0f),
            Vector3f(rect.x.toFloat(), (rect.y + rect.height).toFloat(), 0f),
            Vector3f((rect.x + rect.width).toFloat(), (rect.y + rect.height).toFloat(), 0f),
        )
        corners.forEach(matrix::transformPosition)

        val left = floor(corners.minOf { it.x() }.toDouble()).toInt()
        val top = floor(corners.minOf { it.y() }.toDouble()).toInt()
        val right = ceil(corners.maxOf { it.x() }.toDouble()).toInt()
        val bottom = ceil(corners.maxOf { it.y() }.toDouble()).toInt()
        scissors.push(ScissorStack.Bounds(left, top, right, bottom))
    }

    override fun disableScissor() {
        scissors.pop()
    }

    /** Last-resort recovery for a future faulty/custom visitor; normal visitors finish at depth zero. */
    override fun close() {
        val leakedScissors = scissors.depth
        val leakedTransforms = transformDepth
        while (scissors.depth > 0) scissors.pop()
        while (transformDepth > 0) popTransform()

        if (leakedScissors != 0 || leakedTransforms != 0) {
            Constants.LOG.error(
                "Recovered leaked MCUI render state (transforms={}, scissors={})",
                leakedTransforms,
                leakedScissors,
            )
        }
    }
}
