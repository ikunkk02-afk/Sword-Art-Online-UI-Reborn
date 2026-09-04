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

    companion object {
        val WHITE = ArgbColor(0xFFFFFFFF.toInt())
        val TRANSPARENT = ArgbColor(0x00000000)
    }
}
