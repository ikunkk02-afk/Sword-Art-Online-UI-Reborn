/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.HitResult

/** Reads the current client once at the start of an MCUI HUD frame. */
class HudDataProvider {
    fun capture(minecraft: Minecraft, graphics: GuiGraphics, ticks: DeltaTracker): HudDataSnapshot? {
        val player = minecraft.player ?: return null
        if (minecraft.level == null) return null
        val gameMode = minecraft.gameMode ?: return null

        val inventory = player.inventory
        val hotbarItems = List(HOTBAR_SIZE) { inventory.items[it].copy() }
        val activeEffects = player.activeEffects.mapNotNull { instance ->
            val effect = instance.effect.value()
            val effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect) ?: return@mapNotNull null
            HudEffectSnapshot(
                id = effectId,
                durationTicks = instance.duration,
                amplifier = instance.amplifier,
                ambient = instance.isAmbient,
                visible = instance.isVisible,
                showIcon = instance.showIcon(),
                beneficial = effect.isBeneficial,
            )
        }

        val vehicle = player.vehicle
        val livingMount = vehicle as? LivingEntity
        val jumpableMount = player.jumpableVehicle()
        val hitType = when {
            minecraft.crosshairPickEntity != null -> HudCrosshairTargetType.ENTITY
            minecraft.hitResult?.type == HitResult.Type.BLOCK -> HudCrosshairTargetType.BLOCK
            else -> HudCrosshairTargetType.MISS
        }
        val attackStrength = player.getAttackStrengthScale(0f).coerceIn(0f, 1f)
        val foodData = player.foodData
        val maxAir = player.maxAirSupply.coerceAtLeast(1)

        return HudDataSnapshot(
            playerHealth = player.health,
            playerMaxHealth = player.maxHealth.coerceAtLeast(1f),
            playerAbsorption = player.absorptionAmount,
            armor = player.armorValue,
            food = foodData.foodLevel,
            maxFood = DEFAULT_MAX_FOOD,
            saturation = foodData.saturationLevel,
            maxSaturation = DEFAULT_MAX_SATURATION,
            air = player.airSupply,
            maxAir = maxAir,
            experienceProgress = player.experienceProgress.coerceIn(0f, 1f),
            experienceLevel = player.experienceLevel,
            experienceVisible = gameMode.hasExperience(),
            selectedHotbarSlot = inventory.selected.coerceIn(0, HOTBAR_SIZE - 1),
            hotbarItems = hotbarItems,
            mainHandItem = player.mainHandItem.copy(),
            offHandItem = player.offhandItem.copy(),
            activeEffects = activeEffects,
            riding = player.isPassenger,
            hasLivingMount = livingMount != null,
            mountHealth = livingMount?.health ?: 0f,
            mountMaxHealth = livingMount?.maxHealth?.coerceAtLeast(1f) ?: 1f,
            hasJumpingMount = jumpableMount != null,
            jumpProgress = if (jumpableMount == null) 0f else player.jumpRidingScale.coerceIn(0f, 1f),
            jumpCooldown = jumpableMount?.jumpCooldown ?: 0,
            crosshair = HudCrosshairSnapshot(
                targetType = hitType,
                attackStrength = attackStrength,
                attackReady = attackStrength >= 1f,
            ),
            creative = player.isCreative,
            spectator = player.isSpectator,
            survivalHud = gameMode.canHurtPlayer(),
            underwater = player.isEyeInFluid(FluidTags.WATER),
            dead = !player.isAlive,
            firstPerson = minecraft.options.cameraType.isFirstPerson,
            guiWidth = graphics.guiWidth(),
            guiHeight = graphics.guiHeight(),
            guiScale = minecraft.window.guiScale,
            partialTick = ticks.getGameTimeDeltaPartialTick(true),
        )
    }

    private companion object {
        const val HOTBAR_SIZE = 9
        const val DEFAULT_MAX_FOOD = 20
        const val DEFAULT_MAX_SATURATION = 20f
    }
}
