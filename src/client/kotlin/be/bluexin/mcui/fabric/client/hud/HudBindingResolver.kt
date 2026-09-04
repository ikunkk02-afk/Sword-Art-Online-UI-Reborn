/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.themes.HudItemSource
import be.bluexin.mcui.themes.HudValueSource
import net.minecraft.world.item.ItemStack
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Type-safe bindings used by dynamic HUD elements; no expression engine is involved. */
object HudBindingResolver {
    fun progress(source: HudValueSource, data: HudDataSnapshot): Float = when (source) {
        HudValueSource.PLAYER_HEALTH -> ratio(data.playerHealth, data.playerMaxHealth)
        HudValueSource.PLAYER_MAX_HEALTH -> if (data.playerMaxHealth > 0f) 1f else 0f
        HudValueSource.PLAYER_ABSORPTION -> ratio(data.playerAbsorption, data.playerMaxHealth)
        HudValueSource.FOOD -> ratio(data.food.toFloat(), data.maxFood.toFloat())
        HudValueSource.MAX_FOOD -> if (data.maxFood > 0) 1f else 0f
        HudValueSource.SATURATION -> ratio(data.saturation, data.maxSaturation)
        HudValueSource.MAX_SATURATION -> if (data.maxSaturation > 0f) 1f else 0f
        HudValueSource.AIR -> ratio(data.air.toFloat(), data.maxAir.toFloat())
        HudValueSource.MAX_AIR -> if (data.maxAir > 0) 1f else 0f
        HudValueSource.ARMOR -> ratio(data.armor.toFloat(), MAX_ARMOR)
        HudValueSource.EXPERIENCE_PROGRESS -> data.experienceProgress.coerceIn(0f, 1f)
        HudValueSource.EXPERIENCE_LEVEL -> if (data.experienceLevel > 0) 1f else 0f
        HudValueSource.MOUNT_HEALTH -> ratio(data.mountHealth, data.mountMaxHealth)
        HudValueSource.MOUNT_MAX_HEALTH -> if (data.hasLivingMount) 1f else 0f
        HudValueSource.JUMP_PROGRESS -> data.jumpProgress.coerceIn(0f, 1f)
        HudValueSource.HOTBAR_SELECTED_SLOT -> data.selectedHotbarSlot / 8f
    }

    fun text(source: HudValueSource, data: HudDataSnapshot): String = when (source) {
        HudValueSource.PLAYER_HEALTH -> format(data.playerHealth)
        HudValueSource.PLAYER_MAX_HEALTH -> format(data.playerMaxHealth)
        HudValueSource.PLAYER_ABSORPTION -> format(data.playerAbsorption)
        HudValueSource.FOOD -> data.food.toString()
        HudValueSource.MAX_FOOD -> data.maxFood.toString()
        HudValueSource.SATURATION -> format(data.saturation)
        HudValueSource.MAX_SATURATION -> format(data.maxSaturation)
        HudValueSource.AIR -> data.air.toString()
        HudValueSource.MAX_AIR -> data.maxAir.toString()
        HudValueSource.ARMOR -> data.armor.toString()
        HudValueSource.EXPERIENCE_PROGRESS -> format(data.experienceProgress)
        HudValueSource.EXPERIENCE_LEVEL -> data.experienceLevel.toString()
        HudValueSource.MOUNT_HEALTH -> format(data.mountHealth)
        HudValueSource.MOUNT_MAX_HEALTH -> format(data.mountMaxHealth)
        HudValueSource.JUMP_PROGRESS -> format(data.jumpProgress)
        HudValueSource.HOTBAR_SELECTED_SLOT -> data.selectedHotbarSlot.toString()
    }

    fun item(source: HudItemSource, data: HudDataSnapshot): ItemStack = when (source) {
        HudItemSource.MAIN_HAND -> data.mainHandItem
        HudItemSource.OFF_HAND -> data.offHandItem
        else -> data.hotbarItems.getOrElse(source.hotbarIndex ?: -1) { ItemStack.EMPTY }
    }

    private fun ratio(value: Float, maximum: Float): Float =
        if (!value.isFinite() || !maximum.isFinite() || maximum <= 0f) 0f else (value / maximum).coerceIn(0f, 1f)

    private fun format(value: Float): String {
        if (!value.isFinite()) return "0"
        val rounded = value.roundToInt()
        return if (abs(value - rounded) < 0.001f) rounded.toString()
        else String.format(Locale.ROOT, "%.1f", value)
    }

    private const val MAX_ARMOR = 20f
}
