/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.config.SaoOption
import be.bluexin.mcui.effects.SaoEntityState
import com.tencao.saomclib.capabilities.getPartyCapability
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult

/** Reads the current client once at the start of an MCUI HUD frame. */
class HudDataProvider {
    fun capture(minecraft: Minecraft, graphics: GuiGraphics, ticks: DeltaTracker): HudDataSnapshot? {
        val player = minecraft.player ?: return null
        if (minecraft.level == null) return null
        val gameMode = minecraft.gameMode ?: return null

        val inventory = player.inventory
        val hotbarItems = List(HOTBAR_SIZE) { inventory.items[it].copy() }
        val hotbarItemPopTimes = List(HOTBAR_SIZE) { inventory.items[it].popTime }
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
        val rootVehicle = if (player.isPassenger) player.rootVehicle else null
        val livingMount = (vehicle as? LivingEntity) ?: (rootVehicle as? LivingEntity)
        val showsMountHealth = livingMount?.showVehicleHealth() == true
        val mountSnapshot = livingMount?.takeIf { showsMountHealth }?.let { mount ->
            MountHealthSnapshot(
                entityId = mount.id,
                displayName = mount.displayName?.string ?: mount.name.string,
                health = mount.health,
                maxHealth = mount.maxHealth.coerceAtLeast(1f),
            )
        }
        val jumpableMount = player.jumpableVehicle()
        val hitType = when {
            minecraft.crosshairPickEntity != null -> HudCrosshairTargetType.ENTITY
            minecraft.hitResult?.type == HitResult.Type.BLOCK -> HudCrosshairTargetType.BLOCK
            else -> HudCrosshairTargetType.MISS
        }
        val attackStrength = player.getAttackStrengthScale(0f).coerceIn(0f, 1f)
        val foodData = player.foodData
        val maxAir = player.maxAirSupply.coerceAtLeast(1)
        // IngameGUI.getMouseOver used its own 64-block entity ray, independent
        // of Minecraft's short interaction reach and block crosshair result.
        val camera = minecraft.cameraEntity ?: player
        val eye = camera.getEyePosition(ticks.getGameTimeDeltaPartialTick(true))
        val ray = camera.getViewVector(1f).scale(64.0)
        val tracked = ProjectileUtil.getEntityHitResult(
            camera, eye, eye.add(ray), camera.boundingBox.expandTowards(ray).inflate(1.0),
            { it is LivingEntity && !it.isSpectator && it.isPickable && it.isAlive &&
                it !== vehicle && it !== rootVehicle }, 64.0 * 64.0,
        )?.entity as? LivingEntity
        val targetEntity = tracked?.let { entitySnapshot(it, player) }
        val nearbyEntities = minecraft.level!!.getEntitiesOfClass(
            LivingEntity::class.java,
            AABB(
                player.x - ENTITY_HUD_HORIZONTAL_RANGE,
                player.y - ENTITY_HUD_VERTICAL_RANGE,
                player.z - ENTITY_HUD_HORIZONTAL_RANGE,
                player.x + ENTITY_HUD_HORIZONTAL_RANGE,
                player.y + ENTITY_HUD_VERTICAL_RANGE,
                player.z + ENTITY_HUD_HORIZONTAL_RANGE,
            ),
        ) { entity ->
            entity !== player &&
                entity !== tracked &&
                entity !== vehicle &&
                entity !== rootVehicle &&
                entity.isAlive &&
                isLegacyAggressiveCandidate(entity, player)
        }
            .asSequence()
            .sortedBy { entity -> player.distanceToSqr(entity) }
            .take(MAX_ENTITY_HUD_ENTRIES)
            .map { entity -> entitySnapshot(entity, player) }
            .sortedBy { entity -> entity.health / entity.maxHealth }
            .toList()
        val partyMembers = player.getPartyCapability().partyData?.getMembers().orEmpty()
            .asSequence()
            .filterNot { it.uuid == player.uuid }
            .filterNot { SaoOption.HIDE_OFFLINE_PARTY() && !it.isOnline }
            .map { member ->
                val remotePlayer = member.player as? LivingEntity
                val gameType = minecraft.connection?.getPlayerInfo(member.uuid)?.gameMode
                PartyMemberSnapshot(
                    uuid = member.uuid,
                    displayName = member.username,
                    health = remotePlayer?.health ?: member.health,
                    maxHealth = (remotePlayer?.maxHealth ?: member.maxHealth).coerceAtLeast(1f),
                    online = member.isOnline,
                    creative = gameType == net.minecraft.world.level.GameType.CREATIVE,
                    survivalOrAdventure = gameType == net.minecraft.world.level.GameType.SURVIVAL ||
                        gameType == net.minecraft.world.level.GameType.ADVENTURE,
                )
            }
            .toList()

        return HudDataSnapshot(
            playerName = player.scoreboardName,
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
            hotbarItemPopTimes = hotbarItemPopTimes,
            mainHandItem = player.mainHandItem.copy(),
            offHandItem = player.offhandItem.copy(),
            offHandItemPopTime = player.offhandItem.popTime,
            activeEffects = activeEffects,
            riding = player.isPassenger,
            hasLivingMount = showsMountHealth,
            mountHealth = mountSnapshot?.health ?: 0f,
            mountMaxHealth = mountSnapshot?.maxHealth ?: 1f,
            mountSnapshot = mountSnapshot,
            vehicleEntityId = vehicle?.id,
            rootVehicleEntityId = rootVehicle?.id,
            hasJumpingMount = jumpableMount != null,
            jumpProgress = if (jumpableMount == null) 0f else player.jumpRidingScale.coerceIn(0f, 1f),
            jumpCooldown = jumpableMount?.jumpCooldown ?: 0,
            crosshair = HudCrosshairSnapshot(
                targetType = hitType,
                attackStrength = attackStrength,
                attackReady = attackStrength >= 1f,
            ),
            targetEntity = targetEntity,
            nearbyEntities = nearbyEntities,
            partyMembers = partyMembers,
            creative = player.isCreative,
            spectator = player.isSpectator,
            survivalHud = gameMode.canHurtPlayer(),
            // Legacy StatusEffects.WET used PlayerEntity.isInWater, not eye-fluid state.
            underwater = player.isInWater,
            onFire = player.isOnFire,
            mainArmRight = player.mainArm == net.minecraft.world.entity.HumanoidArm.RIGHT,
            dead = !player.isAlive,
            firstPerson = minecraft.options.cameraType.isFirstPerson,
            guiWidth = graphics.guiWidth(),
            guiHeight = graphics.guiHeight(),
            guiScale = minecraft.window.guiScale,
            partialTick = ticks.getGameTimeDeltaPartialTick(true),
        )
    }

    private fun entitySnapshot(entity: LivingEntity, player: Player): TargetEntitySnapshot = TargetEntitySnapshot(
        entityId = entity.id,
        displayName = entity.displayName?.string ?: entity.name.string,
        health = entity.health,
        maxHealth = entity.maxHealth.coerceAtLeast(1f),
        entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type),
        distance = player.distanceTo(entity),
        alive = entity.isAlive,
        armor = entity.armorValue,
        colorRgb = SaoEntityState.of(entity, player).rgb,
    )

    /** Modern equivalent of the old RenderCapability ColorState.KILLER candidate filter. */
    private fun isLegacyAggressiveCandidate(entity: LivingEntity, player: Player): Boolean =
        SaoEntityState.of(entity, player) == SaoEntityState.KILLER

    private companion object {
        const val HOTBAR_SIZE = 9
        const val DEFAULT_MAX_FOOD = 20
        const val DEFAULT_MAX_SATURATION = 20f
        // IngameGUI.renderEnemyHealth(): AABB(player.pos-10/-5/-10, player.pos+10/+5/+10).
        const val ENTITY_HUD_HORIZONTAL_RANGE = 10.0
        const val ENTITY_HUD_VERTICAL_RANGE = 5.0
        const val MAX_ENTITY_HUD_ENTRIES = 5
    }
}
