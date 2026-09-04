/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.themes.HudPartType

object HudPartVisibility {
    fun isVisible(part: HudPartType, data: HudDataSnapshot): Boolean = when (part) {
        HudPartType.HEALTH_BOX,
        HudPartType.ARMOR -> data.survivalHud && !data.spectator && !data.dead

        HudPartType.FOOD -> data.survivalHud && !data.spectator && !data.dead

        HudPartType.AIR -> data.survivalHud && !data.spectator && !data.dead && data.air < data.maxAir

        HudPartType.HOTBAR -> !data.spectator && !data.dead
        HudPartType.EXPERIENCE -> data.experienceVisible && !data.spectator && !data.dead
        HudPartType.CROSS_HAIR -> data.firstPerson && !data.dead
        HudPartType.EFFECTS -> data.activeEffects.any(HudEffectSnapshot::showIcon) ||
            data.food <= HUNGRY_THRESHOLD || (data.underwater && data.air < data.maxAir) || data.onFire
        HudPartType.MOUNT_HEALTH -> data.hasLivingMount && !data.spectator && !data.dead
        HudPartType.JUMP_BAR -> data.hasJumpingMount && !data.spectator && !data.dead

        HudPartType.ENTITY_HEALTH_HUD -> data.firstPerson && !data.dead && data.nearbyEntities.isNotEmpty()

        HudPartType.AM2BARS,
        HudPartType.PARTY -> false
    }

    private const val HUNGRY_THRESHOLD = 18
}
