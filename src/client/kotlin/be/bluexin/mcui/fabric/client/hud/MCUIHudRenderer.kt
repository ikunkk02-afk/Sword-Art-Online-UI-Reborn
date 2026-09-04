/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.Constants
import be.bluexin.mcui.render.ArgbColor
import be.bluexin.mcui.render.ClipRect
import be.bluexin.mcui.render.MinecraftGuiRenderOperations
import be.bluexin.mcui.render.RenderContext
import be.bluexin.mcui.render.RenderingElementVisitor
import be.bluexin.mcui.render.ResolvedRenderState
import be.bluexin.mcui.render.ResolvedTransform
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.ItemElement
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import be.bluexin.mcui.render.element.TextureElement
import be.bluexin.mcui.util.mcuiId
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.concurrent.atomic.AtomicBoolean

object MCUIHudRenderer {
    private const val SMOKE_TEST_PROPERTY = "mcui.renderSmokeTest"
    private val registered = AtomicBoolean(false)
    private val smokeTestEnabled =
        FabricLoader.getInstance().isDevelopmentEnvironment && java.lang.Boolean.getBoolean(SMOKE_TEST_PROPERTY)
    private val smokeTree by lazy(::createSmokeTree)

    fun register() {
        if (!registered.compareAndSet(false, true)) return
        HudRenderCallback.EVENT.register(HudRenderCallback(::renderHud))
        Constants.LOG.info(
            "MCUI HUD rendering foundation registered (development smoke test: {})",
            smokeTestEnabled,
        )
    }

    private fun renderHud(graphics: net.minecraft.client.gui.GuiGraphics, ticks: net.minecraft.client.DeltaTracker) {
        if (!smokeTestEnabled) return
        val minecraft = Minecraft.getInstance()
        if (minecraft.level == null || minecraft.player == null) return

        MinecraftGuiRenderOperations(graphics, minecraft).use { operations ->
            RenderingElementVisitor(operations).render(
                smokeTree,
                RenderContext(
                    partialTick = ticks.getGameTimeDeltaPartialTick(true),
                    guiWidth = graphics.guiWidth(),
                    guiHeight = graphics.guiHeight(),
                ),
            )
        }
    }

    private fun createSmokeTree() = GroupElement(
        renderState = ResolvedRenderState(name = "phase_2_smoke_root"),
        transform = ResolvedTransform(x = 16f, y = 16f),
        children = listOf(
            RectangleElement(
                transform = ResolvedTransform(z = 0f),
                width = 224,
                height = 78,
                color = ArgbColor(0xB0202630.toInt()),
            ),
            TextElement(
                transform = ResolvedTransform(x = 12f, y = 10f, z = 2f),
                text = Component.literal("Sword Art Online UI: Reborn"),
                shadow = true,
            ),
            TextElement(
                transform = ResolvedTransform(x = 12f, y = 25f, z = 2f, scaleX = 0.9f, scaleY = 0.9f),
                text = Component.literal("Rendering Phase 2"),
                color = ArgbColor(0xFFFFC857.toInt()),
            ),
            TextureElement(
                transform = ResolvedTransform(x = 150f, y = 9f, z = 1f),
                texture = mcuiId("icon.png"),
                width = 60,
                height = 25,
                sourceWidth = 300,
                sourceHeight = 124,
                textureWidth = 300,
                textureHeight = 124,
                tint = ArgbColor(0xE6FFFFFF.toInt()),
            ),
            GroupElement(
                renderState = ResolvedRenderState(
                    name = "nested_clip_probe",
                    clip = ClipRect(0, 0, 112, 10),
                ),
                transform = ResolvedTransform(x = 12f, y = 52f, z = 1f),
                children = listOf(
                    RectangleElement(
                        width = 150,
                        height = 10,
                        color = ArgbColor(0xB0E04B4B.toInt()),
                    ),
                    TextElement(
                        transform = ResolvedTransform(x = 4f, y = 1f, z = 1f, scaleX = 0.75f, scaleY = 0.75f),
                        text = Component.literal("nested scissor restored"),
                    ),
                ),
            ),
            ItemElement(
                transform = ResolvedTransform(x = 194f, y = 51f, z = 3f),
                stack = ItemStack(Items.GOLDEN_APPLE, 3),
            ),
        ),
    )
}
