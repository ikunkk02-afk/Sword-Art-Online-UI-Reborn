/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Static replacement for the legacy disabled death screen; no fade or particle animation. */
class SaoDeathScreen(
    private val causeOfDeath: Component?,
    private val hardcore: Boolean,
) : Screen(Component.translatable(if (hardcore) "deathScreen.title.hardcore" else "deathScreen.title")) {
    override fun init() {
        val y = height / 2 + 62
        if (!hardcore) {
            addRenderableWidget(
                SaoIconButton(
                    width / 2 - 104,
                    y,
                    102,
                    22,
                    Component.translatable("deathScreen.respawn"),
                    SaoIcon.CONFIRM,
                ) {
                    minecraft?.player?.respawn()
                    minecraft?.setScreen(null)
                },
            )
        }
        addRenderableWidget(
            SaoIconButton(
                if (hardcore) width / 2 - 80 else width / 2 + 2,
                y,
                if (hardcore) 160 else 102,
                22,
                Component.translatable("deathScreen.titleScreen"),
                SaoIcon.LOGOUT,
            ) {
                minecraft?.setScreen(
                    SaoConfirmationScreen(
                        parent = this,
                        title = Component.translatable("deathScreen.quit.confirm"),
                        body = Component.translatable("mcui.screen.death.leave"),
                    ) { minecraft?.disconnect(SaoTitleScreen()) },
                )
            },
        )
    }

    override fun shouldCloseOnEsc(): Boolean = false

    override fun isPauseScreen(): Boolean = false

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fillGradient(0, 0, width, height, 0xAA5B0808.toInt(), 0xE015181E.toInt())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val imageWidth = minOf(319, width - 30)
        val imageHeight = imageWidth * 129 / 319
        val imageX = (width - imageWidth) / 2
        val imageY = height / 2 - 94
        graphics.blit(
            SaoUiStyle.DEATH,
            imageX,
            imageY,
            imageWidth,
            imageHeight,
            0f,
            0f,
            319,
            129,
            319,
            129,
        )
        graphics.drawCenteredString(font, title, width / 2, imageY + 10, 0xFFFF6262.toInt())
        causeOfDeath?.let { graphics.drawCenteredString(font, it, width / 2, imageY + imageHeight - 18, SaoUiStyle.LIGHT_TEXT) }
        val score = minecraft?.player?.score ?: 0
        graphics.drawCenteredString(
            font,
            Component.translatable("deathScreen.score", score),
            width / 2,
            imageY + imageHeight - 6,
            SaoUiStyle.MUTED_TEXT,
        )
        super.render(graphics, mouseX, mouseY, partialTick)
    }
}
