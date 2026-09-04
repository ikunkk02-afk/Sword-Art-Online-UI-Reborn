/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/** Renderer-backend-independent drawing vocabulary used by MCUI elements. */
interface GuiRenderOperations : AutoCloseable {
    fun pushTransform()
    fun popTransform()
    fun translate(x: Float, y: Float, z: Float)
    fun scale(x: Float, y: Float)

    fun fill(x: Int, y: Int, width: Int, height: Int, color: ArgbColor)

    fun texture(
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
        tint: ArgbColor = ArgbColor.WHITE,
    )

    fun text(
        text: Component,
        x: Int,
        y: Int,
        color: ArgbColor,
        shadow: Boolean,
        centered: Boolean,
    )

    fun text(
        text: String,
        x: Int,
        y: Int,
        color: ArgbColor,
        shadow: Boolean,
        centered: Boolean,
    )

    fun item(stack: ItemStack, x: Int, y: Int, decorations: Boolean = true, countText: String? = null)

    /** The rectangle is expressed in the current element's local GUI coordinates. */
    fun enableScissor(rect: ClipRect)
    fun disableScissor()
}
