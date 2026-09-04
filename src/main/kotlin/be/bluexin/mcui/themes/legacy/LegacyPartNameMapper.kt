/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes.legacy

import be.bluexin.mcui.themes.HudPartType

/** Normalizes historical spelling without silently accepting unknown HUD parts. */
object LegacyPartNameMapper {
    fun map(rawName: String): HudPartType? {
        val normalized = rawName.trim()
            .replace('-', '_')
            .replace(' ', '_')
            .uppercase()
        return when (normalized) {
            "CROSSHAIR" -> HudPartType.CROSS_HAIR
            "HEALTH", "HEALTHBOX" -> HudPartType.HEALTH_BOX
            "XP", "EXP" -> HudPartType.EXPERIENCE
            "MOUNT", "MOUNT_HEALTH_BAR" -> HudPartType.MOUNT_HEALTH
            "JUMP", "HORSE_JUMP" -> HudPartType.JUMP_BAR
            "ENTITY_HEALTH", "ENTITYHEALTHHUD" -> HudPartType.ENTITY_HEALTH_HUD
            else -> HudPartType.entries.firstOrNull { it.name == normalized }
        }
    }
}
