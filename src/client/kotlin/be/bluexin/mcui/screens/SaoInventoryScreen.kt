/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component

/**
 * Modern interactive inventory: Minecraft keeps slot/click/recipe-book ownership,
 * while the unfinished legacy InventoryGui is represented by the original SAO assets.
 */
class SaoInventoryScreen : InventoryScreen(
    requireNotNull(Minecraft.getInstance().player) { "SAO inventory requires a local player" },
) {
    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        SaoUiStyle.renderPanel(graphics, leftPos - 4, topPos - 4, imageWidth + 8, imageHeight + 8)

        graphics.setColor(1f, 1f, 1f, 0.12f)
        graphics.blit(
            SaoUiStyle.PROFILE_BACKGROUND,
            leftPos,
            topPos,
            imageWidth,
            imageHeight,
            0f,
            0f,
            512,
            512,
            512,
            512,
        )
        graphics.setColor(1f, 1f, 1f, 1f)

        menu.slots.asSequence()
            .filter { it.isActive }
            .forEach { slot -> SaoUiStyle.renderSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1) }

        graphics.fill(leftPos + 7, topPos + 7, leftPos + 80, topPos + 79, 0x382B3038)
        graphics.fill(leftPos + 7, topPos + 78, leftPos + 80, topPos + 79, SaoUiStyle.GOLD)
        val client = Minecraft.getInstance()
        val playerName = client.player?.displayName ?: client.player?.name
        if (playerName != null) {
            graphics.drawCenteredString(font, playerName, leftPos + 43, topPos + 10, SaoUiStyle.LIGHT_TEXT)
        }
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.drawString(font, Component.translatable("sao.element.profile"), titleLabelX, titleLabelY, SaoUiStyle.TEXT, false)
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, SaoUiStyle.MUTED_TEXT, false)
    }
}
