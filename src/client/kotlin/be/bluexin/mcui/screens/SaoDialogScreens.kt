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
) : Screen(title), SaoScreenSurface {
    private val animation = SaoScreenAnimation()
    private val buttons = mutableListOf<SaoIconButton>()
    private var committed = false
    private lateinit var layout: DialogLayout

    override fun init() {
        animation.reset()
        committed = false
        buttons.clear()
        layout = dialogLayout(width, height, 250, 132)
        val horizontal = layout.panelWidth >= 218
        val buttonWidth = if (horizontal) (layout.contentWidth - 4) / 2 else layout.contentWidth
        val firstY = layout.panelY + layout.panelHeight - if (horizontal) 29 else 53
        val confirmButton = SaoIconButton(
            layout.contentX,
            firstY,
            buttonWidth,
            20,
            Component.translatable("gui.yes"),
            SaoIcon.CONFIRM,
        ) {
            if (!committed) {
                committed = true
                buttons.forEach { it.active = false }
                confirmed()
            }
        }
        val cancelButton = SaoIconButton(
            if (horizontal) layout.contentX + buttonWidth + 4 else layout.contentX,
            if (horizontal) firstY else firstY + 24,
            buttonWidth,
            20,
            Component.translatable("gui.no"),
            SaoIcon.CANCEL,
        ) { onClose() }
        buttons += addRenderableWidget(confirmButton)
        buttons += addRenderableWidget(cancelButton)
    }

    override fun onClose() {
        if (!committed) minecraft?.setScreen(parent)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (minecraft?.level == null) SaoUiStyle.renderMenuBackground(graphics, 0, 0, width, height)
        else SaoUiStyle.renderInWorldBackground(graphics, width, height)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        renderDialogPanel(graphics, layout, animation)
        graphics.drawCenteredString(font, SaoUiStyle.fitText(title, layout.contentWidth), width / 2, layout.panelY + 17, SaoUiStyle.TITLE)
        font.split(body, layout.contentWidth).take(4).forEachIndexed { index, line ->
            graphics.drawCenteredString(font, line, width / 2, layout.panelY + 44 + index * 10, SaoUiStyle.MUTED_TEXT)
        }
        val style = SaoUiStyle.current()
        buttons.forEachIndexed { index, button ->
            button.setAlpha(animation.screenProgress(style.animation.screenFadeMillis, index * style.animation.buttonStaggerMillis))
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }
}

class SaoNoticeScreen(private val parent: Screen?) : Screen(Component.literal("Sword Art Online UI: Reborn")), SaoScreenSurface {
    private val animation = SaoScreenAnimation()
    private lateinit var layout: DialogLayout
    private lateinit var doneButton: SaoIconButton

    override fun init() {
        animation.reset()
        layout = dialogLayout(width, height, 300, 154)
        doneButton = addRenderableWidget(
            SaoIconButton(
                layout.contentX,
                layout.panelY + layout.panelHeight - 29,
                layout.contentWidth,
                20,
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
        renderDialogPanel(graphics, layout, animation)
        SaoUiStyle.renderIcon(graphics, SaoIcon.HELP, layout.contentX, layout.panelY + 15, 22)
        graphics.drawString(
            font,
            SaoUiStyle.fitText(title, layout.contentWidth - 30),
            layout.contentX + 30,
            layout.panelY + 20,
            SaoUiStyle.TITLE,
            true,
        )
        val lines = listOf(
            Component.translatable("mcui.screen.notice.framework"),
            Component.translatable("mcui.screen.notice.integrations"),
        ).flatMap { font.split(it, layout.contentWidth) }
        lines.take(5).forEachIndexed { index, line ->
            graphics.drawString(font, line, layout.contentX, layout.panelY + 56 + index * 10, SaoUiStyle.MUTED_TEXT, false)
        }
        doneButton.setAlpha(animation.screenProgress(SaoUiStyle.current().animation.screenFadeMillis))
        super.render(graphics, mouseX, mouseY, partialTick)
    }
}

private data class DialogLayout(
    val panelX: Int,
    val panelY: Int,
    val panelWidth: Int,
    val panelHeight: Int,
    val contentX: Int,
    val contentWidth: Int,
)

private fun dialogLayout(screenWidth: Int, screenHeight: Int, preferredWidth: Int, preferredHeight: Int): DialogLayout {
    val style = SaoUiStyle.current()
    val margin = style.spacing.margin
    val panelWidth = minOf(preferredWidth, (screenWidth - margin * 2).coerceAtLeast(100))
    val panelHeight = minOf(preferredHeight, (screenHeight - margin * 2).coerceAtLeast(100))
    val panelX = (screenWidth - panelWidth) / 2
    val panelY = (screenHeight - panelHeight) / 2
    val padding = style.spacing.padding.coerceAtMost(panelWidth / 4)
    return DialogLayout(panelX, panelY, panelWidth, panelHeight, panelX + padding, panelWidth - padding * 2)
}

private fun renderDialogPanel(graphics: GuiGraphics, layout: DialogLayout, animation: SaoScreenAnimation) {
    val style = SaoUiStyle.current()
    val progress = animation.screenProgress(style.animation.screenFadeMillis)
    val y = layout.panelY + ((1f - progress) * style.animation.panelSlidePixels).toInt()
    SaoUiStyle.renderPanel(graphics, layout.panelX, y, layout.panelWidth, layout.panelHeight, alpha = progress)
    graphics.fill(
        layout.panelX + style.spacing.border,
        y + 1,
        layout.panelX + layout.panelWidth,
        y + layout.panelHeight - 1,
        SaoUiStyle.multiplyAlpha(style.colors.dialog, progress),
    )
    graphics.setColor(1f, 1f, 1f, 0.08f * progress)
    graphics.blit(
        style.textures.dialogBackground,
        layout.panelX,
        y,
        minOf(125, layout.panelWidth),
        layout.panelHeight,
        0f,
        0f,
        256,
        256,
        256,
        256,
    )
    graphics.setColor(1f, 1f, 1f, 1f)
}
