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
 * SAO chrome over the vanilla InventoryScreen. InventoryMenu, slots, recipe book,
 * tooltips, quick-craft and all input handling remain owned by Minecraft.
 */
class SaoInventoryScreen : InventoryScreen(
    requireNotNull(Minecraft.getInstance().player) { "SAO inventory requires a local player" },
), SaoScreenSurface {
    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val style = SaoUiStyle.current()
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, style.colors.panelDark)

        graphics.setColor(1f, 1f, 1f, 0.12f)
        graphics.blit(
            style.textures.profileBackground,
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

        SaoUiStyle.renderPanel(graphics, leftPos + 5, topPos + 5, 74, 76, dark = true)
        SaoUiStyle.renderPanel(graphics, leftPos + 84, topPos + 5, 87, 76, dark = true)
        SaoUiStyle.renderPanel(graphics, leftPos + 5, topPos + 82, 166, 78, dark = true)
        graphics.fill(leftPos + 7, topPos + 78, leftPos + 78, topPos + 80, style.colors.accent)
        graphics.fill(leftPos + 86, topPos + 78, leftPos + 169, topPos + 80, style.colors.slotEquipment)

        val player = Minecraft.getInstance().player
        if (player != null) {
            renderEntityInInventoryFollowsMouse(
                graphics,
                leftPos + 26,
                topPos + 8,
                leftPos + 75,
                topPos + 78,
                30,
                0.0625f,
                mouseX.toFloat(),
                mouseY.toFloat(),
                player,
            )
        }
    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val profile = SaoUiStyle.fitText(Component.translatable("sao.element.profile"), 66)
        val equipment = SaoUiStyle.fitText(Component.translatable("sao.element.equipment"), 78)
        val inventory = SaoUiStyle.fitText(playerInventoryTitle, 154)
        graphics.drawCenteredString(font, profile, 42, 9, SaoUiStyle.LIGHT_TEXT)
        graphics.drawString(font, equipment, 89, 9, SaoUiStyle.TITLE, false)
        graphics.drawString(font, inventory, 9, 72, SaoUiStyle.LIGHT_TEXT, false)

        val playerName = Minecraft.getInstance().player?.displayName ?: Minecraft.getInstance().player?.name
        if (playerName != null) {
            val fitted = SaoUiStyle.fitText(playerName, 66)
            graphics.drawCenteredString(font, fitted, 42, 68, SaoUiStyle.MUTED_TEXT)
        }
    }
}
