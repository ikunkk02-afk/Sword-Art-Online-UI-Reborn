/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * The 1.16.5 NotificationAlert geometry, adapted to the modern toast sprite API.
 *
 * The legacy party/event producers no longer exist, so this class intentionally exposes an
 * explicit entry point instead of inventing replacement notification sources.
 */
class SaoNotificationAlert(
    private val icon: SaoIcon,
    private val title: Component,
    private val subtitle: Component = Component.empty(),
) : Toast {
    private var firstDrawTime = 0L
    private var newDisplay = true

    override fun render(graphics: GuiGraphics, toastComponent: ToastComponent, delta: Long): Toast.Visibility {
        if (newDisplay) {
            firstDrawTime = delta
            newDisplay = false
            SaoSounds.play(SaoSound.MESSAGE)
        }

        graphics.blitSprite(BACKGROUND, 0, 0, width(), height())
        SaoUiStyle.renderIcon(graphics, icon, ICON_X, ICON_Y, ICON_SIZE, LegacySaoMetrics.WHITE)

        val font = toastComponent.minecraft.font
        if (subtitle.string.isEmpty()) {
            graphics.drawString(font, title, TEXT_X, SINGLE_LINE_Y, TITLE_COLOR, false)
        } else {
            graphics.drawString(font, title, TEXT_X, TITLE_Y, TITLE_COLOR, false)
            graphics.drawString(font, subtitle, TEXT_X, SUBTITLE_Y, SUBTITLE_COLOR, false)
        }

        return if (delta - firstDrawTime < DISPLAY_TIME_MS) Toast.Visibility.SHOW else Toast.Visibility.HIDE
    }

    companion object {
        // Original IToast.TEXTURE_TOASTS slice (0,96,160,32) became this named sprite.
        private val BACKGROUND = ResourceLocation.withDefaultNamespace("toast/recipe")
        private const val DISPLAY_TIME_MS = 5_000L
        private const val ICON_X = 6
        private const val ICON_Y = 8
        private const val ICON_SIZE = 16
        private const val TEXT_X = 25
        private const val SINGLE_LINE_Y = 12
        private const val TITLE_Y = 7
        private const val SUBTITLE_Y = 18
        private const val TITLE_COLOR = -11_534_256
        private const val SUBTITLE_COLOR = -16_777_216

        @JvmStatic
        fun show(icon: SaoIcon, title: Component, subtitle: Component = Component.empty()) {
            Minecraft.getInstance().toasts.addToast(SaoNotificationAlert(icon, title, subtitle))
        }
    }
}
