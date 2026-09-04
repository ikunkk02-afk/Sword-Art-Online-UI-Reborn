/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** SAO death presentation with the 1.21.1 vanilla respawn and disconnect lifecycle. */
class SaoDeathScreen(
    private val causeOfDeath: Component?,
    private val hardcore: Boolean,
) : Screen(Component.translatable(if (hardcore) "deathScreen.title.hardcore" else "deathScreen.title")), SaoScreenSurface {
    private val delayedButtons = mutableListOf<SaoIconButton>()
    private var delayTicker = 0
    private lateinit var layout: Layout

    override fun init() {
        delayTicker = 0
        delayedButtons.clear()
        layout = createLayout()
        val primary = SaoIconButton(
            layout.firstButtonX,
            layout.buttonY,
            layout.buttonWidth,
            20,
            Component.translatable(if (hardcore) "deathScreen.spectate" else "deathScreen.respawn"),
            SaoIcon.CONFIRM,
        ) {
            minecraft?.player?.respawn()
            delayedButtons.firstOrNull()?.active = false
        }
        val titleButton = SaoIconButton(
            layout.secondButtonX,
            layout.secondButtonY,
            layout.buttonWidth,
            20,
            Component.translatable("deathScreen.titleScreen"),
            SaoIcon.LOGOUT,
        ) {
            requestExitToTitle()
        }
        primary.active = false
        titleButton.active = false
        delayedButtons += addRenderableWidget(primary)
        delayedButtons += addRenderableWidget(titleButton)
    }

    override fun tick() {
        super.tick()
        delayTicker++
        if (delayTicker == 20) delayedButtons.forEach { it.active = true }
    }

    override fun shouldCloseOnEsc(): Boolean = false

    override fun isPauseScreen(): Boolean = false

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        SaoUiStyle.renderInWorldBackground(graphics, width, height)
        graphics.fillGradient(0, 0, width, height, 0x335B0808, 0x6615181E)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.blit(
            SaoUiStyle.DEATH,
            layout.imageX,
            layout.imageY,
            layout.imageWidth,
            layout.imageHeight,
            0f,
            0f,
            319,
            129,
            319,
            129,
        )
        graphics.drawCenteredString(font, SaoUiStyle.fitText(title, layout.imageWidth - 24), width / 2, layout.imageY + 10, SaoUiStyle.TITLE)
        causeOfDeath?.let { cause ->
            font.split(cause, (layout.imageWidth - 28).coerceAtLeast(20)).take(2).forEachIndexed { index, line ->
                graphics.drawCenteredString(font, line, width / 2, layout.imageY + layout.imageHeight - 28 + index * 9, SaoUiStyle.LIGHT_TEXT)
            }
        }
        val score = minecraft?.player?.score ?: 0
        val scoreText = Component.translatable(
            "deathScreen.score.value",
            Component.literal(score.toString()).withStyle(ChatFormatting.YELLOW),
        )
        graphics.drawCenteredString(font, scoreText, width / 2, layout.imageY + layout.imageHeight - 8, SaoUiStyle.MUTED_TEXT)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun requestExitToTitle() {
        val client = minecraft ?: return
        client.reportingContext.draftReportHandled(
            client,
            this,
            Runnable {
                if (hardcore) {
                    exitToTitle()
                } else {
                    client.setScreen(
                        SaoConfirmationScreen(
                            parent = this,
                            title = Component.translatable("deathScreen.quit.confirm"),
                            body = Component.translatable("mcui.screen.death.leave"),
                        ) { exitToTitle() },
                    )
                }
            },
            true,
        )
    }

    private fun exitToTitle() {
        val client = minecraft ?: return
        client.level?.disconnect()
        client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel")))
        client.setScreen(SaoTitleScreen())
    }

    private fun createLayout(): Layout {
        val style = SaoUiStyle.current()
        val margin = style.spacing.margin
        val horizontalButtons = width >= 236
        val reservedButtons = if (horizontalButtons) 32 else 56
        val maxImageHeight = (height - reservedButtons - margin * 2).coerceAtLeast(48)
        val imageWidth = minOf(319, (width - margin * 2).coerceAtLeast(80), maxImageHeight * 319 / 129)
        val imageHeight = imageWidth * 129 / 319
        val imageY = ((height - imageHeight - reservedButtons) / 2).coerceAtLeast(margin)
        val buttonWidth = if (horizontalButtons) minOf(150, (width - margin * 2 - style.spacing.buttonGap) / 2) else minOf(200, width - margin * 2)
        val firstButtonX = if (horizontalButtons) width / 2 - buttonWidth - style.spacing.buttonGap / 2 else (width - buttonWidth) / 2
        val secondButtonX = if (horizontalButtons) width / 2 + style.spacing.buttonGap / 2 else firstButtonX
        val buttonY = imageY + imageHeight + 7
        return Layout(
            imageX = (width - imageWidth) / 2,
            imageY = imageY,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            firstButtonX = firstButtonX,
            secondButtonX = secondButtonX,
            buttonY = buttonY,
            secondButtonY = if (horizontalButtons) buttonY else buttonY + 24,
            buttonWidth = buttonWidth,
        )
    }

    private data class Layout(
        val imageX: Int,
        val imageY: Int,
        val imageWidth: Int,
        val imageHeight: Int,
        val firstButtonX: Int,
        val secondButtonX: Int,
        val buttonY: Int,
        val secondButtonY: Int,
        val buttonWidth: Int,
    )
}
