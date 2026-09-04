/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Small, deliberately finite easing vocabulary exposed to theme authors. */
@Serializable(with = AnimationEasingSerializer::class)
enum class AnimationEasing {
    UNSUPPORTED,
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    QUAD_IN,
    QUAD_OUT,
    QUAD_IN_OUT,
    CUBIC_IN,
    CUBIC_OUT,
    CUBIC_IN_OUT,
}

@Serializable(with = AnimationPropertySerializer::class)
enum class AnimationProperty {
    UNSUPPORTED,
    ALPHA,
    TRANSLATION_X,
    TRANSLATION_Y,
    SCALE,
    PROGRESS,
    COLOR,
}

@Serializable(with = AnimationTriggerSerializer::class)
enum class AnimationTrigger {
    UNSUPPORTED,
    ON_SHOW,
    ON_HIDE,
    ON_VALUE_CHANGE,
}

/**
 * Theme-side animation data. Values are literal JSON numbers; COLOR interprets them as packed ARGB.
 * ON_VALUE_CHANGE may omit from/to because its endpoints come from the previous/current HUD values.
 */
@Serializable
data class AnimationDefinition(
    val property: AnimationProperty = AnimationProperty.UNSUPPORTED,
    val duration: Int = 200,
    val delay: Int = 0,
    val easing: AnimationEasing = AnimationEasing.LINEAR,
    val from: Double? = null,
    val to: Double? = null,
    val trigger: AnimationTrigger = AnimationTrigger.UNSUPPORTED,
)

/** SAO-style player-health animation parameters and delayed-damage texture. */
@Serializable
data class HealthAnimationDefinition(
    val mainDuration: Int = 160,
    val damageDelay: Int = 240,
    val damageDuration: Int = 420,
    val healDuration: Int = 180,
    val easing: AnimationEasing = AnimationEasing.EASE_OUT,
)

/** Literal-only overrides applied to the root of a fragment instance during resource reload. */
@Serializable
data class FragmentArguments(
    val enabled: Boolean? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val scale: Double? = null,
    val color: ArgbColorDefinition? = null,
    val text: String? = null,
    val texture: String? = null,
    val tint: ArgbColorDefinition? = null,
)

object AnimationEasingSerializer : KSerializer<AnimationEasing> {
    override val descriptor = PrimitiveSerialDescriptor("AnimationEasing", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): AnimationEasing = enumValue(decoder.decodeString(), AnimationEasing.entries)
    override fun serialize(encoder: Encoder, value: AnimationEasing) = encoder.encodeString(value.name)
}

object AnimationPropertySerializer : KSerializer<AnimationProperty> {
    override val descriptor = PrimitiveSerialDescriptor("AnimationProperty", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): AnimationProperty = enumValue(decoder.decodeString(), AnimationProperty.entries)
    override fun serialize(encoder: Encoder, value: AnimationProperty) = encoder.encodeString(value.name)
}

object AnimationTriggerSerializer : KSerializer<AnimationTrigger> {
    override val descriptor = PrimitiveSerialDescriptor("AnimationTrigger", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): AnimationTrigger = enumValue(decoder.decodeString(), AnimationTrigger.entries)
    override fun serialize(encoder: Encoder, value: AnimationTrigger) = encoder.encodeString(value.name)
}

private fun <T : Enum<T>> enumValue(raw: String, values: List<T>): T =
    values.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: values.first()
