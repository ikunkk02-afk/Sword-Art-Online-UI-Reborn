/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import java.util.UUID

/** Immutable, per-frame values consumed by HUD elements. No live player/world object is retained. */
data class HudDataSnapshot(
    val playerName: String,
    val playerHealth: Float,
    val playerMaxHealth: Float,
    val playerAbsorption: Float,
    val armor: Int,
    val food: Int,
    val maxFood: Int,
    val saturation: Float,
    val maxSaturation: Float,
    val air: Int,
    val maxAir: Int,
    val experienceProgress: Float,
    val experienceLevel: Int,
    val experienceVisible: Boolean,
    val selectedHotbarSlot: Int,
    val hotbarItems: List<ItemStack>,
    val hotbarItemPopTimes: List<Int>,
    val mainHandItem: ItemStack,
    val offHandItem: ItemStack,
    val offHandItemPopTime: Int,
    val activeEffects: List<HudEffectSnapshot>,
    val riding: Boolean,
    val hasLivingMount: Boolean,
    val mountHealth: Float,
    val mountMaxHealth: Float,
    val mountSnapshot: MountHealthSnapshot?,
    val vehicleEntityId: Int?,
    val rootVehicleEntityId: Int?,
    val hasJumpingMount: Boolean,
    val jumpProgress: Float,
    val jumpCooldown: Int,
    val crosshair: HudCrosshairSnapshot,
    val targetEntity: TargetEntitySnapshot? = null,
    val nearbyEntities: List<TargetEntitySnapshot> = emptyList(),
    val partyMembers: List<PartyMemberSnapshot> = emptyList(),
    val creative: Boolean,
    val spectator: Boolean,
    val survivalHud: Boolean,
    val underwater: Boolean,
    val dead: Boolean,
    val firstPerson: Boolean,
    val guiWidth: Int,
    val guiHeight: Int,
    val guiScale: Double,
    val partialTick: Float,
    val onFire: Boolean = false,
    val mainArmRight: Boolean = true,
)

/** Immutable SAOMCLib party data used by the original PARTY HUD part. */
data class PartyMemberSnapshot(
    val uuid: UUID,
    val displayName: String,
    val health: Float,
    val maxHealth: Float,
    val online: Boolean,
    val creative: Boolean,
    val survivalOrAdventure: Boolean,
)

/** Safe riding data retained by the animation runtime while the mount HUD exits. */
data class MountHealthSnapshot(
    val entityId: Int,
    val displayName: String,
    val health: Float,
    val maxHealth: Float,
)

data class TargetEntitySnapshot(
    val entityId: Int,
    val displayName: String,
    val health: Float,
    val maxHealth: Float,
    val entityType: ResourceLocation,
    val distance: Float,
    val alive: Boolean,
    val armor: Int? = null,
    val colorRgb: Int = 0xB91111,
)

typealias HudEntitySnapshot = TargetEntitySnapshot

data class HudEffectSnapshot(
    val id: ResourceLocation,
    val durationTicks: Int,
    val amplifier: Int,
    val ambient: Boolean,
    val visible: Boolean,
    val showIcon: Boolean,
    val beneficial: Boolean,
)

data class HudCrosshairSnapshot(
    val targetType: HudCrosshairTargetType,
    val attackStrength: Float,
    val attackReady: Boolean,
)

enum class HudCrosshairTargetType {
    MISS,
    BLOCK,
    ENTITY,
}
