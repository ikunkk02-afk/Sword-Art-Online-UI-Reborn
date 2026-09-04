/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import kotlinx.serialization.Serializable

/** Stable names from the historical MCUI HUD contract. */
@Serializable
enum class HudPartType {
    HEALTH_BOX,
    HOTBAR,
    EXPERIENCE,
    CROSS_HAIR,
    ARMOR,
    JUMP_BAR,
    /** Reserved for the old Ars Magica integration. */
    AM2BARS,
    /** Reserved until the social/party integration is restored. */
    PARTY,
    FOOD,
    EFFECTS,
    AIR,
    MOUNT_HEALTH,
    /** Rendering entry retained; target acquisition is deferred beyond phase four. */
    ENTITY_HEALTH_HUD,
}

/** Anchor origin in Minecraft GUI logical pixels; transform x/y remain offsets. */
@Serializable
enum class HudAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
}

/** Typed numeric inputs. Bars normalize these while text renders their raw value. */
@Serializable
enum class HudValueSource {
    PLAYER_HEALTH,
    PLAYER_MAX_HEALTH,
    PLAYER_ABSORPTION,
    FOOD,
    MAX_FOOD,
    SATURATION,
    MAX_SATURATION,
    AIR,
    MAX_AIR,
    ARMOR,
    EXPERIENCE_PROGRESS,
    EXPERIENCE_LEVEL,
    MOUNT_HEALTH,
    MOUNT_MAX_HEALTH,
    JUMP_PROGRESS,
    HOTBAR_SELECTED_SLOT,
}

@Serializable
enum class HudItemSource {
    HOTBAR_SLOT_0,
    HOTBAR_SLOT_1,
    HOTBAR_SLOT_2,
    HOTBAR_SLOT_3,
    HOTBAR_SLOT_4,
    HOTBAR_SLOT_5,
    HOTBAR_SLOT_6,
    HOTBAR_SLOT_7,
    HOTBAR_SLOT_8,
    MAIN_HAND,
    OFF_HAND;

    val hotbarIndex: Int?
        get() = when (this) {
            HOTBAR_SLOT_0 -> 0
            HOTBAR_SLOT_1 -> 1
            HOTBAR_SLOT_2 -> 2
            HOTBAR_SLOT_3 -> 3
            HOTBAR_SLOT_4 -> 4
            HOTBAR_SLOT_5 -> 5
            HOTBAR_SLOT_6 -> 6
            HOTBAR_SLOT_7 -> 7
            HOTBAR_SLOT_8 -> 8
            MAIN_HAND, OFF_HAND -> null
        }
}

@Serializable
enum class ProgressDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
}
