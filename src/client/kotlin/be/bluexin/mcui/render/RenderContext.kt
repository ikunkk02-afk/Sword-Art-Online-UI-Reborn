/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

/** Values resolved for one HUD frame; no script runtime is involved. */
data class RenderContext(
    val partialTick: Float,
    val guiWidth: Int,
    val guiHeight: Int,
)

/** A transform after future theme expressions have been evaluated. */
data class ResolvedTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
) {
    companion object {
        val IDENTITY = ResolvedTransform()
    }
}

/** A local, GUI-logical-coordinate clipping rectangle. */
data class ClipRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(width >= 0) { "Clip width must be non-negative" }
        require(height >= 0) { "Clip height must be non-negative" }
    }
}

/** Render switches after future MiniScript expressions have been evaluated. */
data class ResolvedRenderState(
    val enabled: Boolean = true,
    val name: String = "",
    val clip: ClipRect? = null,
)
