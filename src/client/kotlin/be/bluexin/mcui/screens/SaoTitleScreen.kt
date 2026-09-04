/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import be.bluexin.mcui.Constants
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.LanguageSelectScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.network.chat.Component

/** Responsive SAO title surface that retains Minecraft's primary entry points. */
class SaoTitleScreen : Screen(Component.literal(Constants.MOD_NAME)), SaoScreenSurface {
    private val animation = SaoScreenAnimation()
    private val buttons = mutableListOf<SaoIconButton>()
    private lateinit var layout: Layout

    override fun init() {
        val client = requireNotNull(minecraft)
        animation.reset()
        buttons.clear()
        layout = createLayout()

        addSaoButton(layout.x, layout.firstY, layout.buttonWidth, Component.translatable("menu.singleplayer"), SaoIcon.PROFILE) {
            client.setScreen(SelectWorldScreen(this))
        }
        addSaoButton(layout.x, layout.firstY + layout.step, layout.buttonWidth, Component.translatable("menu.multiplayer"), SaoIcon.SOCIAL) {
            client.setScreen(JoinMultiplayerScreen(this))
        }.active = client.allowsMultiplayer()
        addSaoButton(layout.x, layout.firstY + layout.step * 2, layout.buttonWidth, Component.translatable("menu.options"), SaoIcon.SETTINGS) {
            client.setScreen(OptionsScreen(this, client.options))
        }

        val half = (layout.buttonWidth - layout.gap).coerceAtLeast(2) / 2
        addSaoButton(layout.x, layout.firstY + layout.step * 3, half, Component.translatable("options.language"), SaoIcon.MESSAGE) {
            client.setScreen(LanguageSelectScreen(this, client.options, client.languageManager))
        }
        addSaoButton(
            layout.x + half + layout.gap,
            layout.firstY + layout.step * 3,
            layout.buttonWidth - half - layout.gap,
            Component.translatable("options.accessibility.title"),
            SaoIcon.HELP,
        ) {
            client.setScreen(AccessibilityOptionsScreen(this, client.options))
        }
        addSaoButton(layout.x, layout.firstY + layout.step * 4, layout.buttonWidth, Component.translatable("menu.quit"), SaoIcon.LOGOUT) {
            client.stop()
        }
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        SaoUiStyle.renderMenuBackground(graphics, 0, 0, width, height)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val style = SaoUiStyle.current()
        val panelProgress = animation.screenProgress(style.animation.screenFadeMillis)
        val panelOffset = ((1f - panelProgress) * style.animation.panelSlidePixels).toInt()
        SaoUiStyle.renderPanel(
            graphics,
            layout.panelX,
            layout.panelY + panelOffset,
            layout.panelWidth,
            layout.panelHeight,
            alpha = panelProgress,
        )

        if (layout.logoWidth > 0 && layout.logoHeight > 0) {
            graphics.blit(
                SaoUiStyle.LOGO,
                (width - layout.logoWidth) / 2,
                layout.logoY,
                layout.logoWidth,
                layout.logoHeight,
                0f,
                0f,
                300,
                124,
                300,
                124,
            )
        }
        buttons.forEachIndexed { index, button ->
            button.setAlpha(
                animation.screenProgress(
                    style.animation.screenFadeMillis,
                    index * style.animation.buttonStaggerMillis,
                ),
            )
        }
        super.render(graphics, mouseX, mouseY, partialTick)
        renderFooter(graphics)
    }

    override fun shouldCloseOnEsc(): Boolean = false

    override fun isPauseScreen(): Boolean = false

    private fun createLayout(): Layout {
        val style = SaoUiStyle.current()
        val margin = style.spacing.margin.coerceAtMost((width / 4).coerceAtLeast(1))
        val gap = style.spacing.buttonGap.coerceAtLeast(3)
        val buttonWidth = minOf(260, (width - margin * 2).coerceAtLeast(1))
        val buttonHeight = 20
        val step = buttonHeight + gap
        val rowsHeight = buttonHeight * 5 + gap * 4
        val footerHeight = 28
        val availableBottom = (height - footerHeight - rowsHeight - margin).coerceAtLeast(margin)
        val firstY = minOf(height / 2 - 10, availableBottom).coerceAtLeast(margin + 42)
        val x = (width - buttonWidth) / 2
        val padding = style.spacing.padding
        val logoAvailableHeight = (firstY - margin - padding - 4).coerceAtLeast(0)
        val logoWidth = minOf(300, (width - margin * 2).coerceAtLeast(0), logoAvailableHeight * 300 / 124)
        val logoHeight = logoWidth * 124 / 300
        return Layout(
            x = x,
            firstY = firstY,
            buttonWidth = buttonWidth,
            gap = gap,
            step = step,
            panelX = x - padding,
            panelY = firstY - padding,
            panelWidth = buttonWidth + padding * 2,
            panelHeight = rowsHeight + padding * 2,
            logoWidth = logoWidth,
            logoHeight = logoHeight,
            logoY = (firstY - padding - logoHeight - 5).coerceAtLeast(margin),
        )
    }

    private fun renderFooter(graphics: GuiGraphics) {
        val minecraftVersion = SharedConstants.getCurrentVersion().name
        val modVersion = FabricLoader.getInstance().getModContainer(Constants.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("unknown")
        val left = Component.literal("Minecraft $minecraftVersion  •  MCUI $modVersion")
        val right = Component.literal("Copyright Mojang AB")
        val y = (height - 12).coerceAtLeast(0)
        graphics.drawString(font, SaoUiStyle.fitText(left, (width / 2 - 12).coerceAtLeast(1)), 6, y, SaoUiStyle.MUTED_TEXT, false)
        val fittedRight = SaoUiStyle.fitText(right, (width / 2 - 12).coerceAtLeast(1))
        graphics.drawString(font, fittedRight, (width - font.width(fittedRight) - 6).coerceAtLeast(6), y, SaoUiStyle.MUTED_TEXT, false)
    }

    private fun addSaoButton(
        x: Int,
        y: Int,
        width: Int,
        label: Component,
        icon: SaoIcon,
        action: () -> Unit,
    ): SaoIconButton {
        val button = SaoIconButton(x, y, width, 20, label, icon, action = action)
        buttons += button
        return addRenderableWidget(button)
    }

    private data class Layout(
        val x: Int,
        val firstY: Int,
        val buttonWidth: Int,
        val gap: Int,
        val step: Int,
        val panelX: Int,
        val panelY: Int,
        val panelWidth: Int,
        val panelHeight: Int,
        val logoWidth: Int,
        val logoHeight: Int,
        val logoY: Int,
    )
}
