/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

data class ResolvedHealthAnimationSpec(
    val mainDurationMillis: Int,
    val damageDelayMillis: Int,
    val damageDurationMillis: Int,
    val healDurationMillis: Int,
    val easing: Easing,
)

data class AnimatedHealthValue(
    val displayRatio: Float,
    val delayedRatio: Float,
    val showDelayedDamage: Boolean,
)

/** Player-health-specific state: fast main bar plus a delayed damage-only ghost. */
class HealthAnimationState {
    private val display = FloatTransition()
    private val delayed = FloatTransition()
    private var target = 0f
    private var initialized = false
    private var damageGhost = false

    fun update(targetRatio: Float, spec: ResolvedHealthAnimationSpec, nowNanos: Long): AnimatedHealthValue {
        display.update(nowNanos)
        delayed.update(nowNanos)
        val next = targetRatio.coerceIn(0f, 1f)
        if (!initialized) {
            initialized = true
            target = next
            display.snap(next)
            delayed.snap(next)
        } else if (next < target - EPSILON) {
            val ghostStart = maxOf(delayed.value, display.value, target)
            target = next
            display.start(display.value, next, nowNanos, spec.mainDurationMillis, easing = spec.easing)
            delayed.start(
                from = ghostStart,
                to = next,
                nowNanos = nowNanos,
                durationMillis = spec.damageDurationMillis,
                delayMillis = spec.damageDelayMillis,
                easing = spec.easing,
            )
            damageGhost = true
        } else if (next > target + EPSILON) {
            target = next
            damageGhost = false
            delayed.snap(next)
            display.start(display.value, next, nowNanos, spec.healDurationMillis, easing = spec.easing)
        } else if (!display.active && !delayed.active) {
            display.snap(next)
            delayed.snap(next)
            damageGhost = false
        }

        if (damageGhost && !delayed.active && delayed.value <= display.value + EPSILON) damageGhost = false
        return AnimatedHealthValue(
            displayRatio = display.value.coerceIn(0f, 1f),
            delayedRatio = delayed.value.coerceIn(0f, 1f),
            showDelayedDamage = damageGhost && delayed.value > display.value + EPSILON,
        )
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
