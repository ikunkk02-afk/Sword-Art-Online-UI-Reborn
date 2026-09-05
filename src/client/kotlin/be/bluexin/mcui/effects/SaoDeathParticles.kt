/* Copyright (C) 2020-2021 Tencao
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package be.bluexin.mcui.effects

import be.bluexin.mcui.config.SaoOption
import be.bluexin.mcui.screens.SaoSound
import be.bluexin.mcui.util.legacyMcuiId
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.client.Minecraft
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.util.Mth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Original registered sprite particle, rather than the unused legacy CUSTOM renderer. */
class SaoDeathParticle(level: ClientLevel, x: Double, y: Double, z: Double, color: Int, sprites: SpriteSet) :
    TextureSheetParticle(level, x, y, z, 0.0, 0.0, 0.0) {
    private val rotationSpeed = (random.nextFloat() + 2f) /
        (if (random.nextBoolean()) 16f else -16f) * PI.toFloat()

    init {
        xd = (random.nextDouble() * 2 - 1) * 0.05
        yd = (random.nextDouble() * 2 - 1) * 0.05
        zd = (random.nextDouble() * 2 - 1) * 0.05
        roll = random.nextFloat() * 2f * PI.toFloat()
        oRoll = roll
        setColor((color shr 16 and 255) / 255f, (color shr 8 and 255) / 255f, (color and 255) / 255f)
        lifetime = (8.0 / (random.nextDouble() * 0.8 + 0.2)).toInt()
        hasPhysics = true
        pickSprite(sprites)
    }

    override fun getLightColor(partialTick: Float) = 0xF000F0
    override fun getQuadSize(partialTick: Float): Float =
        super.getQuadSize(partialTick) * ((age + partialTick) / lifetime * 32f).coerceIn(0f, 1f)
    override fun getRenderType(): ParticleRenderType = ParticleRenderType.PARTICLE_SHEET_LIT

    /** The original shards are upright planes rotating around Y, not camera-facing billboards. */
    override fun render(buffer: VertexConsumer, camera: Camera, partialTick: Float) {
        val cameraPos = camera.position
        val px = (Mth.lerp(partialTick.toDouble(), xo, x) - cameraPos.x).toFloat()
        val py = (Mth.lerp(partialTick.toDouble(), yo, y) - cameraPos.y).toFloat()
        val pz = (Mth.lerp(partialTick.toDouble(), zo, z) - cameraPos.z).toFloat()
        val angle = Mth.lerp(partialTick, oRoll, roll)
        val dx = cos(angle) * getQuadSize(partialTick)
        val dz = sin(angle) * getQuadSize(partialTick)
        val size = getQuadSize(partialTick)
        val light = getLightColor(partialTick)
        fun vertex(x: Float, y: Float, z: Float, u: Float, v: Float) {
            buffer.addVertex(x, y, z).setUv(u, v).setColor(rCol, gCol, bCol, alpha).setLight(light)
        }
        vertex(px - dx, py - size, pz - dz, getU0(), getV1())
        vertex(px - dx, py + size, pz - dz, getU0(), getV0())
        vertex(px + dx, py + size, pz + dz, getU1(), getV0())
        vertex(px + dx, py - size, pz + dz, getU1(), getV1())
    }

    override fun tick() {
        xo = x; yo = y; zo = z
        if (age++ >= lifetime) { remove(); return }
        yd += 0.004
        move(xd, yd, zd)
        xd *= 0.8999999761581421
        yd *= 0.8999999761581421
        zd *= 0.8999999761581421
        oRoll = roll
        roll += PI.toFloat() * rotationSpeed * 2f
        if (onGround) {
            xd *= 0.699999988079071
            zd *= 0.699999988079071
        }
    }
}

object SaoDeathParticles {
    private val type = FabricParticleTypes.simple()
    private val colors = intArrayOf(0x9AFE2E, 0x01FFFF, 0x08088A)
    private var registered = false

    fun register() {
        if (registered) return
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, legacyMcuiId("death_particle"), type)
        ParticleFactoryRegistry.getInstance().register(type, ParticleFactoryRegistry.PendingParticleFactory { sprites ->
            ParticleProvider { _, level, x, y, z, red, green, blue ->
                val color = ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt()
                SaoDeathParticle(level, x, y, z, color, sprites)
            }
        })
        registered = true
    }

    @JvmStatic
    fun onDeathTick(entity: LivingEntity) {
        val level = entity.level() as? ClientLevel ?: return
        if (!registered) return
        if (entity.deathTime == 1 && SaoOption.SOUND_EFFECTS()) {
            level.playLocalSound(entity.x, entity.y, entity.z, SaoSound.PARTICLES_DEATH.event, SoundSource.NEUTRAL, 1f, 1f, false)
        }
        if (entity.deathTime != 18 || !SaoOption.PARTICLES()) return
        val count = (entity.bbWidth * entity.bbHeight * 64f).coerceIn(8f, 128f).toInt()
        repeat(count) { index ->
            val color = colors[index % colors.size]
            val random = level.random
            level.addParticle(type,
                entity.x + entity.bbWidth * (random.nextDouble() * 2 - 1) * 0.75,
                entity.y + entity.bbHeight * random.nextDouble(),
                entity.z + entity.bbWidth * (random.nextDouble() * 2 - 1) * 0.75,
                (color shr 16 and 255) / 255.0, (color shr 8 and 255) / 255.0, (color and 255) / 255.0)
        }
    }
}
