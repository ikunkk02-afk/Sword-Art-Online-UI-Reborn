/* Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé, Tencao
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package be.bluexin.mcui.effects

import be.bluexin.mcui.config.SaoOption
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import com.mojang.math.Axis
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class SaoEntityState(val rgb: Int) {
    INNOCENT(0x93F43E), VIOLENT(0xF49B00), KILLER(0xB91111), BOSS(0xBD0000),
    CREATIVE(0x4CEDC5), OP(0), INVALID(0x8B8B8B), DEV(0x79139E);

    fun healthEnabled() = SaoOption.valueOf("${name}_HEALTH")()
    fun crystalEnabled() = SaoOption.valueOf("${name}_CRYSTAL")()

    companion object {
        fun of(entity: LivingEntity, player: Player): SaoEntityState = when {
            entity is EnderDragon || entity is WitherBoss -> BOSS
            entity is Player -> when (Minecraft.getInstance().connection?.getPlayerInfo(entity.uuid)?.gameMode) {
                GameType.CREATIVE -> CREATIVE
                GameType.SPECTATOR -> INVALID
                else -> if (entity.uuid.toString() in DEVELOPERS) DEV else INNOCENT
            }
            entity is NeutralMob && entity.isAngry -> KILLER
            entity is TamableAnimal && entity.ownerUUID != null -> if (entity.ownerUUID == player.uuid) INNOCENT else VIOLENT
            entity is Enemy -> if (entity.hasLineOfSight(player)) KILLER else VIOLENT
            else -> INNOCENT
        }
        private val DEVELOPERS = setOf("08197bad-1da1-48fd-82f1-9b388c49b6c9", "dc1bced1-26df-4c18-8eca-37484229ded1")
    }
}

/** 1.16.5 StaticRenderer geometry, submitted through Minecraft's buffered renderer. */
object SaoEntityIndicators {
    private val texture = ResourceLocation.fromNamespaceAndPath("saoui", "textures/entities.png")
    private var registered = false

    fun register() {
        if (registered) return
        registered = true
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            val client = Minecraft.getInstance()
            val player = client.player ?: return@register
            if (client.options.hideGui || SaoOption.UI_ONLY()) return@register
            val stack = context.matrixStack() ?: return@register
            val consumers = context.consumers() ?: return@register
            val camera = context.camera()
            val partial = context.tickCounter().getGameTimeDeltaPartialTick(true)
            val buffer = consumers.getBuffer(RenderType.entityTranslucent(texture))
            val neat = FabricLoader.getInstance().isModLoaded("neat")
            context.world().entitiesForRendering().filterIsInstance<LivingEntity>().forEach { entity ->
                if (entity === player || !entity.isAlive || entity.isInvisibleTo(player) ||
                    entity.distanceToSqr(camera.position) > 64.0 * 64.0 ||
                    !player.hasLineOfSight(entity)) return@forEach
                val state = SaoEntityState.of(entity, player)
                stack.pushPose()
                try {
                    stack.translate(
                        entity.xOld + (entity.x - entity.xOld) * partial - camera.position.x,
                        entity.yOld + (entity.y - entity.yOld) * partial - camera.position.y,
                        entity.zOld + (entity.z - entity.zOld) * partial - camera.position.z,
                    )
                    val light = client.entityRenderDispatcher.getPackedLightCoords(entity, partial)
                    if (state.crystalEnabled() && !entity.isPassenger) crystal(stack, buffer, entity, state.rgb, light, camera.yRot, partial)
                    if (state.healthEnabled() && entity.vehicle !== player && entity.health <= entity.maxHealth && !neat) {
                        health(stack.last(), buffer, entity, camera.yRot, light)
                    }
                } finally { stack.popPose() }
            }
        }
    }

    private fun crystal(stack: PoseStack, buffer: VertexConsumer, entity: LivingEntity, color: Int, light: Int, yaw: Float, partial: Float) {
        val size = if (entity.isBaby && entity is Enemy) 0.5f else 1f
        stack.pushPose()
        try {
            stack.translate(0f, size * entity.bbHeight + size * 1.1f, 0f)
            stack.mulPose(Axis.YP.rotationDegrees(-yaw))
            val scale = 0.016666668f * 1.6f * size
            stack.scale(-scale, -scale, scale)
            val angle = if (SaoOption.SPINNING_CRYSTALS()) (entity.level().gameTime % 40 + partial) / 20.0 * PI else 0.0
            val c = cos(angle).toFloat(); val s = sin(angle).toFloat()
            plane(stack.last(), buffer, -9f * c, -9f * s, 9f * c, 9f * s, color, light)
            if (SaoOption.SPINNING_CRYSTALS()) plane(stack.last(), buffer, 9f * s, -9f * c, -9f * s, 9f * c, color, light)
        } finally { stack.popPose() }
    }

    private fun plane(pose: PoseStack.Pose, buffer: VertexConsumer, x1: Float, z1: Float, x2: Float, z2: Float, color: Int, light: Int) {
        vertex(pose, buffer, x1, -1f, z1, 0f, 0.25f, color, light)
        vertex(pose, buffer, x1, 17f, z1, 0f, 0.375f, color, light)
        vertex(pose, buffer, x2, 17f, z2, 0.125f, 0.375f, color, light)
        vertex(pose, buffer, x2, -1f, z2, 0.125f, 0.25f, color, light)
    }

    private fun health(pose: PoseStack.Pose, buffer: VertexConsumer, entity: LivingEntity, yaw: Float, light: Int) {
        val ratio = (entity.health / entity.maxHealth).coerceIn(0f, 1f)
        val segments = ((ratio + (1f - ratio) * (1f - ratio) * 0.5f * ratio) * 32).toInt()
        val color = when {
            entity is Player && entity.isCreative -> 0x4CEDC5
            ratio <= 0.1f -> 0xBD0000
            ratio <= 0.2f -> 0xF40000
            ratio <= 0.3f -> 0xF47800
            ratio <= 0.4f -> 0xF4BD00
            ratio <= 0.5f -> 0xEDEB38
            else -> 0x93F43E
        }
        val size = if (entity.isBaby && entity is Enemy) 0.5f else 1f
        val radius = size * entity.bbWidth * 0.975f
        val baseY = size * entity.bbHeight * 0.75f
        fun strip(start: Int, tint: Int, v: Float) {
            for (i in start until 32) {
                val a = Math.toRadians((yaw - 135).toDouble()) + (i / 32.0 - 0.5) * PI * 0.35
                val b = Math.toRadians((yaw - 135).toDouble()) + ((i + 1) / 32.0 - 0.5) * PI * 0.35
                val x1 = radius * cos(a).toFloat(); val z1 = radius * sin(a).toFloat()
                val x2 = radius * cos(b).toFloat(); val z2 = radius * sin(b).toFloat()
                val u1 = 1f - (i - start) / 32f; val u2 = 1f - (i + 1 - start) / 32f
                vertex(pose, buffer, x1, baseY + 0.21f, z1, u1, v, tint, light)
                vertex(pose, buffer, x1, baseY, z1, u1, v + 0.125f, tint, light)
                vertex(pose, buffer, x2, baseY, z2, u2, v + 0.125f, tint, light)
                vertex(pose, buffer, x2, baseY + 0.21f, z2, u2, v, tint, light)
            }
        }
        strip(32 - segments, color, 0f)
        strip(0, 0xFFFFFF, 0.125f)
    }

    private fun vertex(pose: PoseStack.Pose, buffer: VertexConsumer, x: Float, y: Float, z: Float, u: Float, v: Float, rgb: Int, light: Int) {
        buffer.addVertex(pose.pose(), x, y, z).setColor(rgb or (0xFF shl 24)).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0f, 1f, 0f)
    }
}
