/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.render

/**
 * Tracks MCUI's view of nested scissors while GuiGraphics performs the actual
 * GUI-scale conversion and maintains its own matching native stack.
 */
internal class ScissorStack(
    private val enableNative: (Bounds) -> Unit,
    private val disableNative: () -> Unit,
) {
    private val stack = ArrayDeque<Bounds>()

    val depth: Int get() = stack.size

    fun push(bounds: Bounds) {
        val effective = stack.lastOrNull()?.intersection(bounds) ?: bounds
        enableNative(effective)
        stack.addLast(effective)
    }

    fun pop() {
        check(stack.isNotEmpty()) { "MCUI scissor stack underflow" }
        disableNative()
        stack.removeLast()
    }

    data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        init {
            require(right >= left) { "Scissor right edge must not precede its left edge" }
            require(bottom >= top) { "Scissor bottom edge must not precede its top edge" }
        }

        fun intersection(other: Bounds): Bounds {
            val newLeft = maxOf(left, other.left)
            val newTop = maxOf(top, other.top)
            val newRight = maxOf(newLeft, minOf(right, other.right))
            val newBottom = maxOf(newTop, minOf(bottom, other.bottom))
            return Bounds(newLeft, newTop, newRight, newBottom)
        }
    }
}
