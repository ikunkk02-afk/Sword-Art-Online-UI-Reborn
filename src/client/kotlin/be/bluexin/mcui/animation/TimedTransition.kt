/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

/** Normalized 0..1 timed transition used for non-float interpolation such as ARGB. */
class TimedTransition {
    private val transition = FloatTransition()

    val progress: Float get() = transition.value
    val active: Boolean get() = transition.active

    fun start(nowNanos: Long, durationMillis: Int, delayMillis: Int, easing: Easing) {
        transition.start(0f, 1f, nowNanos, durationMillis, delayMillis, easing)
    }

    fun update(nowNanos: Long): Boolean = transition.update(nowNanos)
}
