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
import be.bluexin.mcui.render.MinecraftGuiRenderOperations
import be.bluexin.mcui.render.RenderContext
import be.bluexin.mcui.render.RenderingElementVisitor
import be.bluexin.mcui.themes.MCUIThemes
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import java.util.concurrent.atomic.AtomicBoolean

object MCUIHudRenderer {
    private val registered = AtomicBoolean(false)

    fun register() {
        if (!registered.compareAndSet(false, true)) return
        HudRenderCallback.EVENT.register(HudRenderCallback(::renderHud))
        Constants.LOG.info("MCUI resource-driven HUD renderer registered")
    }

    private fun renderHud(graphics: net.minecraft.client.gui.GuiGraphics, ticks: net.minecraft.client.DeltaTracker) {
        val minecraft = Minecraft.getInstance()
        if (minecraft.level == null || minecraft.player == null) return
        val root = MCUIThemes.manager.activeTheme.hudRoot

        MinecraftGuiRenderOperations(graphics, minecraft).use { operations ->
            RenderingElementVisitor(operations).render(
                root,
                RenderContext(
                    partialTick = ticks.getGameTimeDeltaPartialTick(true),
                    guiWidth = graphics.guiWidth(),
                    guiHeight = graphics.guiHeight(),
                ),
            )
        }
    }
}
