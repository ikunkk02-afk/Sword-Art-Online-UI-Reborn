/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

/** MCUI GUI colors are always encoded as 0xAARRGGBB. */
@JvmInline
value class ArgbColor(val value: Int) {
    val alpha: Float get() = channel(24)
    val red: Float get() = channel(16)
    val green: Float get() = channel(8)
    val blue: Float get() = channel(0)

    private fun channel(shift: Int): Float = ((value ushr shift) and 0xFF) / 255f

    fun multiplyAlpha(multiplier: Float): ArgbColor {
        val alpha = (((value ushr 24) and 0xFF) * multiplier.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return ArgbColor((alpha shl 24) or (value and 0x00FFFFFF))
    }

    companion object {
        val WHITE = ArgbColor(0xFFFFFFFF.toInt())
        val TRANSPARENT = ArgbColor(0x00000000)

        fun lerp(from: ArgbColor, to: ArgbColor, progress: Float): ArgbColor {
            val t = progress.coerceIn(0f, 1f)
            fun channel(shift: Int): Int {
                val start = (from.value ushr shift) and 0xFF
                val end = (to.value ushr shift) and 0xFF
                return (start + (end - start) * t).toInt().coerceIn(0, 255)
            }
            return ArgbColor(
                (channel(24) shl 24) or
                    (channel(16) shl 16) or
                    (channel(8) shl 8) or
                    channel(0),
            )
        }
    }
}
