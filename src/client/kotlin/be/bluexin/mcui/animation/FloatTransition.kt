/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

/** Allocation-free, interruptible transition for one float property. */
class FloatTransition(initialValue: Float = 0f) {
    var value: Float = initialValue
        private set
    var target: Float = initialValue
        private set
    var active: Boolean = false
        private set

    private var from = initialValue
    private var startedAt = 0L
    private var delayNanos = 0L
    private var durationNanos = 0L
    private var easing: Easing = Easing.resolve(be.bluexin.mcui.themes.AnimationEasing.LINEAR)

    fun snap(value: Float) {
        this.value = value
        target = value
        active = false
    }

    fun start(
        from: Float = value,
        to: Float,
        nowNanos: Long,
        durationMillis: Int,
        delayMillis: Int = 0,
        easing: Easing,
    ) {
        this.from = from
        value = from
        target = to
        startedAt = nowNanos
        delayNanos = delayMillis.coerceAtLeast(0) * NANOS_PER_MILLI
        durationNanos = durationMillis.coerceAtLeast(0) * NANOS_PER_MILLI
        this.easing = easing
        active = delayNanos > 0L || durationNanos > 0L
        if (!active) value = to
    }

    /** Returns true while more updates are required. */
    fun update(nowNanos: Long): Boolean {
        if (!active) return false
        val elapsed = nowNanos - startedAt
        if (elapsed <= delayNanos) {
            value = from
            return true
        }
        if (durationNanos <= 0L) {
            value = target
            active = false
            return false
        }
        val progress = ((elapsed - delayNanos).toDouble() / durationNanos).toFloat().coerceIn(0f, 1f)
        value = from + (target - from) * easing.transform(progress)
        if (progress >= 1f) {
            value = target
            active = false
        }
        return active
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
