/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

class SaoIconButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
    private val icon: SaoIcon?,
    private val compact: Boolean = false,
    var selected: Boolean = false,
    private val action: () -> Unit,
) : AbstractButton(x, y, width, height, message) {
    override fun onPress() = action()

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        SaoUiStyle.renderButton(
            graphics = graphics,
            x = x,
            y = y,
            width = width,
            height = height,
            message = message,
            hovered = isHovered,
            focused = isFocused,
            pressed = isHovered && Minecraft.getInstance().mouseHandler.isLeftPressed,
            active = active,
            selected = selected,
            alpha = alpha,
            icon = icon,
            compact = compact,
        )
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        defaultButtonNarrationText(output)
    }
}
