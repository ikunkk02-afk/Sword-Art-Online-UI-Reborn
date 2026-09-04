/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.network.chat.Component

/** Source-derived 1.16.5 DeathGui presentation with modern safe lifecycle calls. */
class SaoDeathScreen(
    @Suppress("UNUSED_PARAMETER") causeOfDeath: Component?,
    private val hardcore: Boolean,
) : Screen(Component.translatable(if (hardcore) "deathScreen.title.hardcore" else "deathScreen.title")), SaoScreenSurface {
    private var counter = 0
    private var confirmed = false

    override fun init() {
        counter = 0
        confirmed = false
    }

    override fun tick() {
        if (counter < LegacySaoMetrics.DEATH_FADE_TICKS) counter++
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // DeathGui.render() supplied its own counter/40 black overlay.
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val alpha = (counter / LegacySaoMetrics.DEATH_FADE_TICKS.toFloat()).coerceIn(0f, 1f)
        graphics.fill(0, 0, width, height, ((alpha * 255f).toInt() shl 24))
        val x = width / 2 - LegacySaoMetrics.DEATH_WIDTH / 2
        val y = height / 2 - LegacySaoMetrics.DEATH_HEIGHT / 2
        val tint = if (hardcore) LegacySaoMetrics.HARDCORE_DEATH else LegacySaoMetrics.DEATH
        graphics.setColor(
            (tint ushr 16 and 0xFF) / 255f,
            (tint ushr 8 and 0xFF) / 255f,
            (tint and 0xFF) / 255f,
            1f,
        )
        graphics.blit(
            SaoUiStyle.DEATH,
            x,
            y,
            LegacySaoMetrics.DEATH_WIDTH,
            LegacySaoMetrics.DEATH_HEIGHT,
            0f,
            0f,
            319,
            129,
            319,
            129,
        )
        graphics.setColor(1f, 1f, 1f, 1f)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        confirm()
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) {
            confirm()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun shouldCloseOnEsc(): Boolean = false

    override fun isPauseScreen(): Boolean = true

    private fun confirm() {
        if (confirmed) return
        confirmed = true
        counter = 0
        val client = minecraft ?: return
        if (!hardcore) {
            client.player?.respawn()
            client.setScreen(null)
            return
        }

        val localServer = client.isLocalServer
        val server = client.currentServer
        client.level?.disconnect()
        if (localServer) client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel"))) else client.disconnect()
        val title = SaoTitleScreen()
        client.setScreen(if (localServer || server?.isRealm == true) title else JoinMultiplayerScreen(title))
    }
}
