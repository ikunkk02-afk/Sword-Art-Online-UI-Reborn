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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Serialized and normalized as Minecraft GUI `0xAARRGGBB`. */
@Serializable(with = ArgbColorDefinitionSerializer::class)
@JvmInline
value class ArgbColorDefinition(val value: Int) {
    companion object {
        val WHITE = ArgbColorDefinition(0xFFFFFFFF.toInt())
    }
}

object ArgbColorDefinitionSerializer : KSerializer<ArgbColorDefinition> {
    override val descriptor = PrimitiveSerialDescriptor("ArgbColor", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ArgbColorDefinition {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ARGB colors require JSON input")
        val primitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive
            ?: throw SerializationException("ARGB color must be a string or integer")

        val value = if (primitive.isString) {
            parseString(primitive.content)
        } else {
            parseNumber(primitive)
        }
        return ArgbColorDefinition(value)
    }

    override fun serialize(encoder: Encoder, value: ArgbColorDefinition) {
        encoder.encodeString("#%08X".format(value.value))
    }

    private fun parseString(raw: String): Int {
        val normalized = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        val argb = when (normalized.length) {
            6 -> "FF$normalized"
            8 -> normalized
            else -> throw SerializationException(
                "ARGB color '$raw' must contain 6 (RGB) or 8 (ARGB) hexadecimal digits",
            )
        }
        if (!argb.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            throw SerializationException("ARGB color '$raw' contains a non-hexadecimal character")
        }
        return argb.toLong(16).toInt()
    }

    private fun parseNumber(primitive: JsonPrimitive): Int {
        val numeric = primitive.longOrNull
            ?: throw SerializationException("ARGB color '${primitive.content}' is not an integer")
        if (numeric !in Int.MIN_VALUE.toLong()..0xFFFF_FFFFL) {
            throw SerializationException("ARGB color '$numeric' is outside the 32-bit range")
        }
        return numeric.toInt()
    }
}
