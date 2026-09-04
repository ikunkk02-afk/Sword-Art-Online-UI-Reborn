/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

/** One monotonic timestamp is sampled for an entire HUD frame. */
class AnimationClock(
    private val timeSource: () -> Long = System::nanoTime,
) {
    var nowNanos: Long = 0L
        private set
    var deltaSeconds: Float = 0f
        private set

    fun advance(): Long {
        val sampled = timeSource()
        deltaSeconds = if (nowNanos == 0L || sampled <= nowNanos) 0f
        else ((sampled - nowNanos) / NANOS_PER_SECOND).toFloat()
        nowNanos = sampled
        return sampled
    }

    fun reset() {
        nowNanos = 0L
        deltaSeconds = 0f
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
