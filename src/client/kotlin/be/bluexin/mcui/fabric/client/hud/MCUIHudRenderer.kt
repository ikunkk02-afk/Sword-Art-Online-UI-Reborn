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
import be.bluexin.mcui.themes.MCUIThemes
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import java.util.concurrent.atomic.AtomicBoolean

object MCUIHudRenderer {
    private val registered = AtomicBoolean(false)
    private val dataProvider = HudDataProvider()
    private val coordinator = HudRenderCoordinator()

    fun register() {
        if (!registered.compareAndSet(false, true)) return
        HudRenderCallback.EVENT.register(HudRenderCallback(::renderHud))
        Constants.LOG.info("MCUI HUD part renderer and vanilla replacement policy registered")
    }

    private fun renderHud(graphics: net.minecraft.client.gui.GuiGraphics, ticks: net.minecraft.client.DeltaTracker) {
        val minecraft = Minecraft.getInstance()
        val data = dataProvider.capture(minecraft, graphics, ticks) ?: return
        coordinator.render(MCUIThemes.manager.activeTheme.hud, data, graphics, minecraft)
    }
}
