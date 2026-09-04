/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

import be.bluexin.mcui.themes.AnimationTrigger

enum class HudPartLifecycle {
    HIDDEN,
    ENTERING,
    VISIBLE,
    EXITING,
}

/** Visibility intent and visual lifecycle are deliberately separate. */
class HudAnimationState {
    var lifecycle: HudPartLifecycle = HudPartLifecycle.HIDDEN
        private set
    private var targetVisible = false
    private var transitionEndsAt = 0L
    private var initialized = false

    fun setTarget(visible: Boolean, specs: List<ResolvedAnimationSpec>, nowNanos: Long) {
        if (initialized && visible == targetVisible) return
        if (!initialized && !visible) {
            initialized = true
            targetVisible = false
            lifecycle = HudPartLifecycle.HIDDEN
            transitionEndsAt = nowNanos
            return
        }
        initialized = true
        targetVisible = visible
        val trigger = if (visible) AnimationTrigger.ON_SHOW else AnimationTrigger.ON_HIDE
        val totalMillis = specs.asSequence()
            .filter { it.trigger == trigger }
            .maxOfOrNull { it.delayMillis + it.durationMillis }
            ?: 0
        transitionEndsAt = nowNanos + totalMillis * NANOS_PER_MILLI
        lifecycle = when {
            visible && totalMillis > 0 -> HudPartLifecycle.ENTERING
            visible -> HudPartLifecycle.VISIBLE
            !visible && totalMillis > 0 -> HudPartLifecycle.EXITING
            else -> HudPartLifecycle.HIDDEN
        }
    }

    fun update(nowNanos: Long) {
        if (nowNanos < transitionEndsAt) return
        lifecycle = when (lifecycle) {
            HudPartLifecycle.ENTERING -> HudPartLifecycle.VISIBLE
            HudPartLifecycle.EXITING -> HudPartLifecycle.HIDDEN
            else -> lifecycle
        }
    }

    val shouldRender: Boolean get() = lifecycle != HudPartLifecycle.HIDDEN

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
