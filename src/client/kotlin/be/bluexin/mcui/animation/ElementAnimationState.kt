/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

import be.bluexin.mcui.render.ArgbColor
import be.bluexin.mcui.themes.AnimationProperty
import be.bluexin.mcui.themes.AnimationTrigger

data class ResolvedAnimationSpec(
    val property: AnimationProperty,
    val durationMillis: Int,
    val delayMillis: Int,
    val easing: Easing,
    val from: Double?,
    val to: Double?,
    val trigger: AnimationTrigger,
)

data class AnimatedRenderState(
    val alpha: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1f,
    val color: ArgbColor? = null,
)

/** Mutable runtime values for one stable resolved-element path. */
class ElementAnimationState {
    private val alpha = FloatTransition(1f)
    private val translationX = FloatTransition(0f)
    private val translationY = FloatTransition(0f)
    private val scale = FloatTransition(1f)
    private val colorProgress = TimedTransition()
    private var colorFrom: ArgbColor? = null
    private var colorTo: ArgbColor? = null
    private var initialized = false

    var visibleTarget: Boolean = false
        private set

    fun setVisible(visible: Boolean, specs: List<ResolvedAnimationSpec>, nowNanos: Long): Boolean {
        if (initialized && visible == visibleTarget) return isActive
        val firstTransition = !initialized
        initialized = true
        visibleTarget = visible
        if (firstTransition && !visible) {
            prepareHidden(specs)
            return false
        }
        val trigger = if (visible) AnimationTrigger.ON_SHOW else AnimationTrigger.ON_HIDE
        val matching = specs.filter { it.trigger == trigger }
        if (matching.isEmpty()) {
            if (visible) resetVisible() else prepareHidden(specs)
            return false
        }
        matching.forEach { spec ->
            start(spec, nowNanos, firstTransition)
        }
        return isActive
    }

    fun update(nowNanos: Long): Boolean {
        alpha.update(nowNanos)
        translationX.update(nowNanos)
        translationY.update(nowNanos)
        scale.update(nowNanos)
        colorProgress.update(nowNanos)
        return isActive
    }

    fun snapshot(): AnimatedRenderState = AnimatedRenderState(
        alpha = alpha.value.coerceIn(0f, 1f),
        translationX = translationX.value,
        translationY = translationY.value,
        scale = scale.value.coerceAtLeast(0f),
        color = colorFrom?.let { from -> colorTo?.let { to -> ArgbColor.lerp(from, to, colorProgress.progress) } },
    )

    val isActive: Boolean
        get() = alpha.active || translationX.active || translationY.active || scale.active || colorProgress.active

    private fun prepareHidden(specs: List<ResolvedAnimationSpec>) {
        val setup = specs.filter { it.trigger == AnimationTrigger.ON_SHOW }.ifEmpty {
            specs.filter { it.trigger == AnimationTrigger.ON_HIDE }
        }
        setup.forEach { spec ->
            val value = when {
                spec.trigger == AnimationTrigger.ON_SHOW -> spec.from
                else -> spec.to
            }?.toFloat() ?: when (spec.property) {
                AnimationProperty.ALPHA -> 0f
                AnimationProperty.SCALE -> 1f
                else -> 0f
            }
            when (spec.property) {
                AnimationProperty.ALPHA -> alpha.snap(value)
                AnimationProperty.TRANSLATION_X -> translationX.snap(value)
                AnimationProperty.TRANSLATION_Y -> translationY.snap(value)
                AnimationProperty.SCALE -> scale.snap(value)
                AnimationProperty.COLOR -> {
                    colorFrom = ArgbColor((spec.from ?: spec.to ?: 0.0).toLong().toInt())
                    colorTo = colorFrom
                }
                AnimationProperty.PROGRESS, AnimationProperty.UNSUPPORTED -> Unit
            }
        }
    }

    private fun resetVisible() {
        alpha.snap(1f)
        translationX.snap(0f)
        translationY.snap(0f)
        scale.snap(1f)
        colorFrom = null
        colorTo = null
    }

    private fun start(spec: ResolvedAnimationSpec, nowNanos: Long, firstTransition: Boolean) {
        val defaultFrom = when (spec.property) {
            AnimationProperty.UNSUPPORTED -> 0f
            AnimationProperty.ALPHA -> if (spec.trigger == AnimationTrigger.ON_SHOW) 0f else 1f
            AnimationProperty.SCALE -> 1f
            AnimationProperty.TRANSLATION_X, AnimationProperty.TRANSLATION_Y -> 0f
            AnimationProperty.PROGRESS, AnimationProperty.COLOR -> 0f
        }
        val defaultTo = when (spec.property) {
            AnimationProperty.UNSUPPORTED -> 0f
            AnimationProperty.ALPHA -> if (spec.trigger == AnimationTrigger.ON_HIDE) 0f else 1f
            AnimationProperty.SCALE -> 1f
            AnimationProperty.TRANSLATION_X, AnimationProperty.TRANSLATION_Y -> 0f
            AnimationProperty.PROGRESS, AnimationProperty.COLOR -> 1f
        }
        val from = spec.from?.toFloat() ?: defaultFrom
        val to = spec.to?.toFloat() ?: defaultTo
        val useCurrent = initialized && !firstTransition
        when (spec.property) {
            AnimationProperty.UNSUPPORTED -> Unit
            AnimationProperty.ALPHA -> alpha.start(if (useCurrent) alpha.value else from, to, nowNanos, spec.durationMillis, spec.delayMillis, spec.easing)
            AnimationProperty.TRANSLATION_X -> translationX.start(if (useCurrent) translationX.value else from, to, nowNanos, spec.durationMillis, spec.delayMillis, spec.easing)
            AnimationProperty.TRANSLATION_Y -> translationY.start(if (useCurrent) translationY.value else from, to, nowNanos, spec.durationMillis, spec.delayMillis, spec.easing)
            AnimationProperty.SCALE -> scale.start(if (useCurrent) scale.value else from, to, nowNanos, spec.durationMillis, spec.delayMillis, spec.easing)
            AnimationProperty.COLOR -> {
                colorFrom = ArgbColor((spec.from ?: 0.0).toLong().toInt())
                colorTo = ArgbColor((spec.to ?: 0.0).toLong().toInt())
                colorProgress.start(nowNanos, spec.durationMillis, spec.delayMillis, spec.easing)
            }
            AnimationProperty.PROGRESS -> Unit
        }
    }
}
