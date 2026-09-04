/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import be.bluexin.mcui.Constants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.LanguageSelectScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.network.chat.Component

/** Static SAO title screen built from the stable branch's original logo and icon set. */
class SaoTitleScreen : Screen(Component.literal(Constants.MOD_NAME)) {
    override fun init() {
        val client = requireNotNull(minecraft)
        val buttonWidth = minOf(220, width - 40)
        val x = (width - buttonWidth) / 2
        val firstY = minOf(height - 5 * 23 - 24, height / 2 - 15).coerceAtLeast(100)
        val spacing = 23

        addSaoButton(x, firstY, buttonWidth, Component.translatable("menu.singleplayer"), SaoIcon.PROFILE) {
            client.setScreen(SelectWorldScreen(this))
        }
        addSaoButton(x, firstY + spacing, buttonWidth, Component.translatable("menu.multiplayer"), SaoIcon.SOCIAL) {
            client.setScreen(JoinMultiplayerScreen(this))
        }
        addSaoButton(x, firstY + spacing * 2, buttonWidth, Component.translatable("menu.options"), SaoIcon.SETTINGS) {
            client.setScreen(OptionsScreen(this, client.options))
        }

        val half = (buttonWidth - 4) / 2
        addSaoButton(x, firstY + spacing * 3, half, Component.translatable("options.language"), SaoIcon.MESSAGE) {
            client.setScreen(LanguageSelectScreen(this, client.options, client.languageManager))
        }
        addSaoButton(x + half + 4, firstY + spacing * 3, half, Component.translatable("options.accessibility.title"), SaoIcon.HELP) {
            client.setScreen(AccessibilityOptionsScreen(this, client.options))
        }
        addSaoButton(x, firstY + spacing * 4, buttonWidth, Component.translatable("menu.quit"), SaoIcon.LOGOUT) {
            client.stop()
        }
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        SaoUiStyle.renderMenuBackground(graphics, 0, 0, width, height)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val logoWidth = minOf(300, width - 40, (maxOf(60, firstWidgetY() - 20) * 300 / 124))
        val logoHeight = logoWidth * 124 / 300
        graphics.blit(
            SaoUiStyle.LOGO,
            (width - logoWidth) / 2,
            maxOf(8, firstWidgetY() - logoHeight - 14),
            logoWidth,
            logoHeight,
            0f,
            0f,
            300,
            124,
            300,
            124,
        )
        super.render(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, Constants.MOD_NAME, width / 2, height - 22, SaoUiStyle.MUTED_TEXT)
    }

    override fun shouldCloseOnEsc(): Boolean = false

    override fun isPauseScreen(): Boolean = false

    private fun firstWidgetY(): Int = minOf(height - 5 * 23 - 24, height / 2 - 15).coerceAtLeast(100)

    private fun addSaoButton(x: Int, y: Int, width: Int, label: Component, icon: SaoIcon, action: () -> Unit) {
        addRenderableWidget(SaoIconButton(x, y, width, 20, label, icon, action = action))
    }
}
