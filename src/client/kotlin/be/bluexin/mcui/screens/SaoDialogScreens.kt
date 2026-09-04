/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class SaoConfirmationScreen(
    private val parent: Screen?,
    title: Component,
    private val body: Component,
    private val confirmed: () -> Unit,
) : Screen(title) {
    override fun init() {
        val x = width / 2 - 104
        val y = height / 2 + 26
        addRenderableWidget(
            SaoIconButton(x, y, 102, 22, Component.translatable("gui.yes"), SaoIcon.CONFIRM) {
                confirmed()
            },
        )
        addRenderableWidget(
            SaoIconButton(x + 106, y, 102, 22, Component.translatable("gui.no"), SaoIcon.CANCEL) {
                minecraft?.setScreen(parent)
            },
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (minecraft?.level == null) SaoUiStyle.renderMenuBackground(graphics, 0, 0, width, height)
        else SaoUiStyle.renderInWorldBackground(graphics, width, height)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val panelX = width / 2 - 120
        val panelY = height / 2 - 60
        SaoUiStyle.renderPanel(graphics, panelX, panelY, 240, 125)
        graphics.setColor(1f, 1f, 1f, 0.1f)
        graphics.blit(SaoUiStyle.ALERT_BACKGROUND, panelX, panelY, 125, 125, 0f, 0f, 256, 256, 256, 256)
        graphics.setColor(1f, 1f, 1f, 1f)
        graphics.drawCenteredString(font, title, width / 2, panelY + 18, SaoUiStyle.TEXT)
        graphics.drawCenteredString(font, body, width / 2, panelY + 43, SaoUiStyle.MUTED_TEXT)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
}

class SaoNoticeScreen(private val parent: Screen?) : Screen(Component.literal("Sword Art Online UI: Reborn")) {
    override fun init() {
        addRenderableWidget(
            SaoIconButton(
                width / 2 - 80,
                height / 2 + 36,
                160,
                22,
                Component.translatable("gui.done"),
                SaoIcon.CONFIRM,
            ) { onClose() },
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (minecraft?.level == null) SaoUiStyle.renderMenuBackground(graphics, 0, 0, width, height)
        else SaoUiStyle.renderInWorldBackground(graphics, width, height)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val panelX = width / 2 - 145
        val panelY = height / 2 - 72
        SaoUiStyle.renderPanel(graphics, panelX, panelY, 290, 145)
        SaoUiStyle.renderIcon(graphics, SaoIcon.HELP, panelX + 18, panelY + 18, 24)
        graphics.drawString(font, title, panelX + 52, panelY + 20, SaoUiStyle.TEXT, true)
        graphics.drawString(font, Component.translatable("mcui.screen.notice.static"), panelX + 18, panelY + 57, SaoUiStyle.MUTED_TEXT, false)
        graphics.drawString(font, Component.translatable("mcui.screen.notice.integrations"), panelX + 18, panelY + 73, SaoUiStyle.MUTED_TEXT, false)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
}
