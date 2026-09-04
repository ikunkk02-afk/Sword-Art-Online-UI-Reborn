/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

import be.bluexin.mcui.Constants
import be.bluexin.mcui.fabric.client.hud.HudEffectSnapshot
import be.bluexin.mcui.fabric.client.hud.TargetEntitySnapshot
import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.themes.AnimationProperty
import be.bluexin.mcui.themes.AnimationTrigger
import be.bluexin.mcui.themes.HudPartType
import net.minecraft.resources.ResourceLocation

data class EffectVisualSnapshot(
    val stableKey: String,
    val effect: HudEffectSnapshot?,
    val texture: ResourceLocation,
    val beneficial: Boolean,
)

data class AnimatedEffectVisual(
    val visual: EffectVisualSnapshot,
    val animation: AnimatedRenderState,
)

/** Owns all mutable HUD animation state and clears it atomically on theme reload. */
class AnimationRegistry(
    private val clock: AnimationClock = AnimationClock(),
) {
    private val elementStates = HashMap<String, ElementAnimationState>()
    private val activeElementKeys = LinkedHashSet<String>()
    private val partStates = HashMap<HudPartType, HudAnimationState>()
    private val progressStates = HashMap<String, FloatTransition>()
    private val healthStates = HashMap<String, HealthAnimationState>()
    private val hotbarStates = HashMap<String, FloatTransition>()
    private val effectTrackers = HashMap<String, EffectTracker>()
    private val loggedFailures = HashSet<String>()
    private var themeRevision = Long.MIN_VALUE
    private var themeId = "unknown"
    private var currentTarget: TargetEntitySnapshot? = null
    private var targetLastSeenAt = 0L

    var nowNanos: Long = 0L
        private set

    fun beginFrame(
        revision: Long,
        themeId: String,
        rawTarget: TargetEntitySnapshot?,
        nearbyEntities: List<TargetEntitySnapshot> = emptyList(),
    ) {
        if (revision != themeRevision || themeId != this.themeId) reset(revision)
        this.themeId = themeId
        nowNanos = clock.advance()
        updateActiveElements()
        updateTarget(rawTarget, nearbyEntities)
    }

    fun shouldRenderPart(part: HudPartType, root: Element, visibilityTarget: Boolean): Boolean = guarded(
        key = root.renderState.key,
        property = "VISIBILITY",
        fallback = { visibilityTarget },
    ) {
        val state = partStates.getOrPut(part, ::HudAnimationState)
        state.update(nowNanos)
        state.setTarget(visibilityTarget, root.renderState.animations, nowNanos)
        elementRuntime(root.renderState.key, root.renderState.animations, visibilityTarget)
        state.shouldRender
    }

    fun elementRuntime(key: String, specs: List<ResolvedAnimationSpec>, visible: Boolean): AnimatedRenderState = guarded(
        key = key,
        property = "ELEMENT",
        fallback = { AnimatedRenderState() },
    ) {
        if (specs.isEmpty()) return@guarded AnimatedRenderState()
        val state = elementStates.getOrPut(key, ::ElementAnimationState)
        if (state.setVisible(visible, specs, nowNanos)) activeElementKeys += key
        state.snapshot()
    }

    fun progressValue(key: String, target: Float, specs: List<ResolvedAnimationSpec>): Float = guarded(
        key = key,
        property = "PROGRESS",
        fallback = { target },
    ) {
        val spec = specs.firstOrNull {
            it.property == AnimationProperty.PROGRESS && it.trigger == AnimationTrigger.ON_VALUE_CHANGE
        } ?: return@guarded target
        val transition = progressStates.getOrPut(key) { FloatTransition(target) }
        transition.update(nowNanos)
        if (kotlin.math.abs(target - transition.target) > VALUE_EPSILON) {
            transition.start(transition.value, target, nowNanos, spec.durationMillis, spec.delayMillis, spec.easing)
        }
        transition.value.coerceIn(0f, 1f)
    }

    fun healthValue(key: String, target: Float, spec: ResolvedHealthAnimationSpec): AnimatedHealthValue = guarded(
        key = key,
        property = "HEALTH",
        fallback = { AnimatedHealthValue(target, target, false) },
    ) {
        healthStates.getOrPut(key, ::HealthAnimationState).update(target, spec, nowNanos)
    }

    fun hotbarPosition(key: String, selectedSlot: Int, specs: List<ResolvedAnimationSpec>): Float = guarded(
        key = key,
        property = "HOTBAR_SELECTION",
        fallback = { selectedSlot.toFloat() },
    ) {
        val property = specs.firstOrNull {
            it.trigger == AnimationTrigger.ON_VALUE_CHANGE &&
                (it.property == AnimationProperty.TRANSLATION_X || it.property == AnimationProperty.TRANSLATION_Y)
        } ?: return@guarded selectedSlot.toFloat()
        val target = selectedSlot.toFloat()
        val transition = hotbarStates.getOrPut(key) { FloatTransition(target) }
        transition.update(nowNanos)
        if (kotlin.math.abs(target - transition.target) > VALUE_EPSILON) {
            transition.start(transition.value, target, nowNanos, property.durationMillis, property.delayMillis, property.easing)
        }
        transition.value
    }

    fun effectVisuals(
        key: String,
        current: List<EffectVisualSnapshot>,
        specs: List<ResolvedAnimationSpec>,
    ): Collection<AnimatedEffectVisual> = guarded(
        key = key,
        property = "EFFECT_ENTRY",
        fallback = { current.map { AnimatedEffectVisual(it, AnimatedRenderState()) } },
    ) {
        effectTrackers.getOrPut(key, ::EffectTracker).synchronize(current, specs, nowNanos)
    }

    fun targetEntity(lingerMillis: Int = DEFAULT_TARGET_LINGER_MILLIS): TargetEntitySnapshot? {
        if (currentTarget != null && nowNanos - targetLastSeenAt > lingerMillis.coerceAtLeast(0) * NANOS_PER_MILLI) {
            currentTarget = null
        }
        return currentTarget
    }

    private fun reset(revision: Long) {
        themeRevision = revision
        elementStates.clear()
        activeElementKeys.clear()
        partStates.clear()
        progressStates.clear()
        healthStates.clear()
        hotbarStates.clear()
        effectTrackers.clear()
        loggedFailures.clear()
        currentTarget = null
        targetLastSeenAt = 0L
        clock.reset()
    }

    private fun updateActiveElements() {
        val iterator = activeElementKeys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val state = elementStates[key]
            if (state == null || !state.update(nowNanos)) iterator.remove()
        }
    }

    private fun updateTarget(rawTarget: TargetEntitySnapshot?, nearbyEntities: List<TargetEntitySnapshot>) {
        if (rawTarget != null && rawTarget.alive) {
            currentTarget = rawTarget
            targetLastSeenAt = nowNanos
        } else {
            val targetId = currentTarget?.entityId ?: return
            nearbyEntities.firstOrNull { it.entityId == targetId }?.let { currentTarget = it }
        }
    }

    private inline fun <T> guarded(key: String, property: String, fallback: () -> T, block: () -> T): T = try {
        block()
    } catch (cause: Exception) {
        if (loggedFailures.add(key)) {
            Constants.LOG.error(
                "Disabled faulty HUD animation state (theme={}, element/property={}) after: {}",
                themeId,
                "$key/$property",
                cause.message,
                cause,
            )
        }
        fallback()
    }

    private class EffectTracker {
        private val entries = LinkedHashMap<String, Entry>()
        private var generation = 0L

        fun synchronize(
            current: List<EffectVisualSnapshot>,
            specs: List<ResolvedAnimationSpec>,
            nowNanos: Long,
        ): Collection<AnimatedEffectVisual> {
            generation++
            current.forEach { visual ->
                val entry = entries[visual.stableKey]
                if (entry == null) {
                    val state = ElementAnimationState()
                    state.setVisible(true, specs, nowNanos)
                    entries[visual.stableKey] = Entry(visual, state, generation)
                } else {
                    entry.visual = visual
                    entry.seenGeneration = generation
                    entry.removeAt = Long.MAX_VALUE
                    entry.state.setVisible(true, specs, nowNanos)
                }
            }

            val hideMillis = specs.asSequence()
                .filter { it.trigger == AnimationTrigger.ON_HIDE }
                .maxOfOrNull { it.delayMillis + it.durationMillis }
                ?: 0
            val iterator = entries.values.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                entry.state.update(nowNanos)
                if (entry.seenGeneration != generation && entry.removeAt == Long.MAX_VALUE) {
                    entry.state.setVisible(false, specs, nowNanos)
                    entry.removeAt = nowNanos + hideMillis * NANOS_PER_MILLI
                }
                if (entry.seenGeneration != generation && nowNanos >= entry.removeAt) iterator.remove()
            }
            return entries.values.map { AnimatedEffectVisual(it.visual, it.state.snapshot()) }
        }

        private data class Entry(
            var visual: EffectVisualSnapshot,
            val state: ElementAnimationState,
            var seenGeneration: Long,
            var removeAt: Long = Long.MAX_VALUE,
        )
    }

    private companion object {
        const val VALUE_EPSILON = 0.0001f
        const val NANOS_PER_MILLI = 1_000_000L
        const val DEFAULT_TARGET_LINGER_MILLIS = 3_000
    }
}
