/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.roundToInt

class SaoConfirmationScreen(
    parent: Screen?,
    title: Component,
    body: Component,
    confirmed: () -> Unit,
) : LegacyPopupScreen(
    parent = parent,
    header = title,
    lines = listOf(body),
    footer = Component.empty(),
    buttons = listOf(
        PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER, confirmed),
        PopupButton(SaoIcon.CANCEL, LegacySaoMetrics.CANCEL, LegacySaoMetrics.CANCEL_HOVER, null),
    ),
)

class SaoNoticeScreen(parent: Screen?) : LegacyPopupScreen(
    parent = parent,
    header = Component.literal("Sword Art Online UI: Reborn"),
    lines = listOf(
        Component.translatable("mcui.screen.notice.framework"),
        Component.translatable("mcui.screen.notice.integrations"),
    ),
    footer = Component.empty(),
    buttons = listOf(
        PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER, null),
    ),
)

open class LegacyPopupScreen(
    private val parent: Screen?,
    private val header: Component,
    private val lines: List<Component>,
    private val footer: Component,
    private val buttons: List<PopupButton>,
    private val footerSupplier: (() -> Component)? = null,
) : Screen(header), SaoScreenSurface {
    private var openedAt = 0L
    private var closingAt: Long? = null
    private var closeAction: (() -> Unit)? = null
    private var centerX = 0.0
    private var centerY = 0.0
    private var mouseSet = false
    private var previousMouseX = 0.0
    private var previousMouseY = 0.0

    override fun init() {
        openedAt = System.nanoTime()
        closingAt = null
        closeAction = null
        centerX = width / 2.0
        centerY = height / 2.0
        mouseSet = false
        previousMouseX = 0.0
        previousMouseY = 0.0
        SaoSounds.play(SaoSound.MESSAGE)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        (parent as? SaoIngameMenuScreen)?.renderBehindPopup(graphics)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val now = System.nanoTime()
        val expansion = openProgress(now)
        val eol = closeProgress(now)
        if (closingAt != null && eol <= 0f) {
            finishClose()
            return
        }

        val popupHeight = LegacySaoMetrics.POPUP_BASE_HEIGHT -
            LegacySaoMetrics.POPUP_OPEN_HEIGHT_LOSS * (1f - expansion) +
            LegacySaoMetrics.POPUP_LINE_HEIGHT_GAIN * (lines.size + 2)
        val alpha = if (expansion < 1f) expansion else eol
        val earlyScale = if (expansion < LegacySaoMetrics.POPUP_EARLY_SCALE_THRESHOLD) {
            expansion * LegacySaoMetrics.POPUP_EARLY_SCALE_MULTIPLIER + LegacySaoMetrics.POPUP_EARLY_SCALE_THRESHOLD
        } else 1f

        graphics.pose().pushPose()
        val renderCenterX = centerX.roundToInt()
        val renderCenterY = centerY.roundToInt()
        graphics.pose().translate(renderCenterX.toDouble(), renderCenterY.toDouble(), 0.0)
        graphics.pose().scale(earlyScale * eol, earlyScale, 1f)
        drawPopupBands(graphics, popupHeight, expansion, alpha)
        drawPopupText(graphics, popupHeight, expansion, alpha)
        graphics.pose().popPose()

        // Popup.render popped its own scale before CoreGUI rendered the child IconElements.
        graphics.pose().pushPose()
        graphics.pose().translate(renderCenterX.toDouble(), renderCenterY.toDouble(), 0.0)
        drawButtons(graphics, mouseX, mouseY, expansion, eol)
        graphics.pose().popPose()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0 || closingAt != null) return true
        val progress = openProgress(System.nanoTime())
        buttons.forEachIndexed { index, popupButton ->
            val (x, y) = buttonPosition(index, progress)
            // Legacy IconElement hit testing ignored its visual scale animation.
            val localX = mouseX - centerX
            val localY = mouseY - centerY
            if (localX >= x && localX < x + LegacySaoMetrics.ICON_BOUND &&
                localY >= y && localY < y + LegacySaoMetrics.ICON_BOUND
            ) {
                if (popupButton.closeOnClick) requestClose(popupButton.action) else popupButton.action?.invoke()
                return true
            }
        }
        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (!mouseSet) {
            previousMouseX = mouseX
            previousMouseY = mouseY
            mouseSet = true
        }
        centerX += mouseX - previousMouseX
        centerY += mouseY - previousMouseY
        previousMouseX = mouseX
        previousMouseY = mouseY
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        mouseSet = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun onClose() {
        requestClose(null)
    }

    override fun isPauseScreen(): Boolean = parent?.isPauseScreen ?: false

    private fun requestClose(action: (() -> Unit)?) {
        if (closingAt != null) return
        closingAt = System.nanoTime()
        closeAction = action
        SaoSounds.play(SaoSound.DIALOG_CLOSE)
    }

    private fun finishClose() {
        val action = closeAction
        closingAt = null
        closeAction = null
        if (action != null) {
            action()
        } else {
            (parent as? SaoIngameMenuScreen)?.preparePopupReturn()
            minecraft?.setScreen(parent)
        }
    }

    private fun openProgress(now: Long): Float {
        val linear = ((now - openedAt).coerceAtLeast(0L) / 1_000_000.0 / LegacySaoMetrics.POPUP_OPEN_MILLIS)
            .toFloat().coerceIn(0f, 1f)
        return cubicBezier(
            linear,
            LegacySaoMetrics.POPUP_EASING_X1,
            LegacySaoMetrics.POPUP_EASING_Y1,
            LegacySaoMetrics.POPUP_EASING_X2,
            LegacySaoMetrics.POPUP_EASING_Y2,
        ).toFloat()
    }

    private fun closeProgress(now: Long): Float {
        val started = closingAt ?: return 1f
        val linear = ((now - started).coerceAtLeast(0L) / 1_000_000.0 / LegacySaoMetrics.POPUP_CLOSE_MILLIS)
            .toFloat().coerceIn(0f, 1f)
        return 1f - linear
    }

    private fun drawPopupBands(graphics: GuiGraphics, popupHeight: Float, expansion: Float, alpha: Float) {
        val shadows = if (expansion > LegacySaoMetrics.POPUP_SHADOW_SWITCH) {
            LegacySaoMetrics.POPUP_SHADOW_HEIGHT.toFloat()
        } else LegacySaoMetrics.POPUP_OPEN_SHADOW_HEIGHT * expansion
        val titleHeight = LegacySaoMetrics.POPUP_TITLE_HEIGHT.toFloat()
        val textHeight = max(
            LegacySaoMetrics.POPUP_TEXT_EXPANSION_HEIGHT -
                LegacySaoMetrics.POPUP_TEXT_OPEN_LOSS * (1f - expansion),
            0f,
        ) * lines.size / 2f
        val buttonHeight = LegacySaoMetrics.POPUP_BUTTON_BAND_HEIGHT.toFloat()
        val x = -LegacySaoMetrics.POPUP_WIDTH / 2
        var y = (-popupHeight / 2f).roundToInt()
        setAlpha(graphics, alpha)
        blitBand(
            graphics, x, y, LegacySaoMetrics.POPUP_WIDTH, titleHeight.roundToInt(),
            LegacySaoMetrics.POPUP_TITLE_SOURCE_V, LegacySaoMetrics.POPUP_TITLE_SOURCE_HEIGHT,
        )
        y += titleHeight.roundToInt()
        blitBand(
            graphics, x, y, LegacySaoMetrics.POPUP_WIDTH, shadows.roundToInt(),
            LegacySaoMetrics.POPUP_TOP_SHADOW_SOURCE_V, LegacySaoMetrics.POPUP_SHADOW_SOURCE_HEIGHT,
        )
        y += shadows.roundToInt()
        blitBand(
            graphics, x, y, LegacySaoMetrics.POPUP_WIDTH, textHeight.roundToInt(),
            LegacySaoMetrics.POPUP_TEXT_SOURCE_V, LegacySaoMetrics.POPUP_SHADOW_SOURCE_HEIGHT,
        )
        y += textHeight.roundToInt()
        blitBand(
            graphics, x, y, LegacySaoMetrics.POPUP_WIDTH, shadows.roundToInt(),
            LegacySaoMetrics.POPUP_BOTTOM_SHADOW_SOURCE_V, LegacySaoMetrics.POPUP_SHADOW_SOURCE_HEIGHT,
        )
        y += shadows.roundToInt()
        blitBand(
            graphics, x, y, LegacySaoMetrics.POPUP_WIDTH, buttonHeight.roundToInt(),
            LegacySaoMetrics.POPUP_BUTTON_SOURCE_V, LegacySaoMetrics.POPUP_BUTTON_SOURCE_HEIGHT,
        )
        resetColor(graphics)
    }

    private fun blitBand(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        v: Int,
        sourceHeight: Int,
    ) {
        if (height <= 0) return
        graphics.blit(
            SaoUiStyle.ALERT_BACKGROUND,
            x,
            y,
            width,
            height,
            0f,
            v.toFloat(),
            LegacySaoMetrics.LEGACY_ATLAS_SIZE,
            sourceHeight,
            LegacySaoMetrics.LEGACY_ATLAS_SIZE,
            LegacySaoMetrics.LEGACY_ATLAS_SIZE,
        )
    }

    private fun drawPopupText(graphics: GuiGraphics, popupHeight: Float, expansion: Float, alpha: Float) {
        val shadows = if (expansion > LegacySaoMetrics.POPUP_SHADOW_SWITCH) {
            LegacySaoMetrics.POPUP_SHADOW_HEIGHT.toFloat()
        } else LegacySaoMetrics.POPUP_OPEN_SHADOW_HEIGHT * expansion
        val textHeight = max(
            LegacySaoMetrics.POPUP_TEXT_EXPANSION_HEIGHT -
                LegacySaoMetrics.POPUP_TEXT_OPEN_LOSS * (1f - expansion),
            0f,
        ) * lines.size / 2f
        val top = -popupHeight / 2f
        if (alpha > LegacySaoMetrics.CAN_DRAW_ALPHA) {
            graphics.drawCenteredString(
                font, header, 0,
                (top + LegacySaoMetrics.POPUP_TITLE_HEIGHT / 2f).roundToInt(),
                withAlpha(LegacySaoMetrics.POPUP_TEXT, alpha),
            )
            val currentFooter = footerSupplier?.invoke() ?: footer
            if (currentFooter.string.isNotEmpty()) {
                val footerY = top + LegacySaoMetrics.POPUP_TITLE_HEIGHT + shadows + textHeight +
                    LegacySaoMetrics.POPUP_BUTTON_BAND_HEIGHT / 2f
                graphics.drawCenteredString(font, currentFooter, 0, footerY.roundToInt(), withAlpha(LegacySaoMetrics.POPUP_TEXT, alpha))
            }
        }
        if (alpha > LegacySaoMetrics.POPUP_TEXT_VISIBLE_ALPHA && lines.isNotEmpty()) {
            val lineAlpha = ((alpha - LegacySaoMetrics.POPUP_TEXT_FADE_OFFSET) /
                LegacySaoMetrics.POPUP_TEXT_FADE_OFFSET).coerceIn(0f, 1f)
            lines.forEachIndexed { index, line ->
                val lineY = top + LegacySaoMetrics.POPUP_TITLE_HEIGHT + shadows +
                    textHeight / lines.size * (index + LegacySaoMetrics.POPUP_TEXT_FADE_OFFSET)
                graphics.drawCenteredString(font, line, 0, lineY.roundToInt(), withAlpha(LegacySaoMetrics.DEFAULT_TEXT, lineAlpha))
            }
        }
    }

    private fun drawButtons(graphics: GuiGraphics, mouseX: Int, mouseY: Int, expansion: Float, eol: Float) {
        val alpha = expansion * eol
        buttons.forEachIndexed { index, button ->
            val (x, y) = buttonPosition(index, expansion)
            val screenX = centerX.roundToInt() + x
            val screenY = centerY.roundToInt() + y
            val hovered = mouseX >= screenX && mouseX < screenX + LegacySaoMetrics.ICON_BOUND &&
                mouseY >= screenY && mouseY < screenY + LegacySaoMetrics.ICON_BOUND
            val color = if (hovered) button.hoverColor else button.color
            graphics.pose().pushPose()
            // IconElement.scale was applied about the Popup/CoreGUI origin, not the icon center.
            val buttonScale = if (expansion < 0.2f) expansion * 4f + 0.2f else 1f
            graphics.pose().scale(buttonScale * eol, buttonScale, 1f)
            setColor(graphics, color, alpha)
            graphics.blit(
                LEGACY_GUI, x, y, LegacySaoMetrics.ICON_SIZE, LegacySaoMetrics.ICON_SIZE,
                LegacySaoMetrics.ICON_BACKGROUND_U.toFloat(), LegacySaoMetrics.ICON_BACKGROUND_V.toFloat(),
                LegacySaoMetrics.ICON_SIZE, LegacySaoMetrics.ICON_SIZE,
                LegacySaoMetrics.LEGACY_ATLAS_SIZE, LegacySaoMetrics.LEGACY_ATLAS_SIZE,
            )
            resetColor(graphics)
            if (button.label == null) SaoUiStyle.renderIcon(
                graphics, button.icon,
                x + LegacySaoMetrics.ICON_CONTENT_OFFSET,
                y + LegacySaoMetrics.ICON_CONTENT_OFFSET,
                LegacySaoMetrics.ICON_CONTENT_SIZE,
                LegacySaoMetrics.WHITE,
                alpha,
            )
            button.label?.let { graphics.drawCenteredString(font, it, x + 9, y + 5, LegacySaoMetrics.WHITE) }
            graphics.pose().popPose()
        }
    }

    private fun buttonPosition(index: Int, expansion: Float): Pair<Int, Int> {
        val separation = LegacySaoMetrics.POPUP_WIDTH.toDouble() / buttons.size
        val x = -LegacySaoMetrics.POPUP_WIDTH / 2.0 + separation / 2.0 -
            LegacySaoMetrics.POPUP_BUTTON_HALF_SIZE + separation * index
        val destinationY = LegacySaoMetrics.POPUP_BUTTON_DESTINATION_Y.toDouble() +
            LegacySaoMetrics.POPUP_BUTTON_LINE_Y * lines.size
        val y = LegacySaoMetrics.POPUP_BUTTON_INITIAL_Y +
            (destinationY - LegacySaoMetrics.POPUP_BUTTON_INITIAL_Y) * expansion
        return x.roundToInt() to y.roundToInt()
    }

    private fun setAlpha(graphics: GuiGraphics, alpha: Float) = graphics.setColor(1f, 1f, 1f, alpha.coerceIn(0f, 1f))

    private fun setColor(graphics: GuiGraphics, color: Int, alpha: Float) = graphics.setColor(
        (color ushr 16 and 0xFF) / 255f,
        (color ushr 8 and 0xFF) / 255f,
        (color and 0xFF) / 255f,
        (color ushr 24 and 0xFF) / 255f * alpha.coerceIn(0f, 1f),
    )

    private fun resetColor(graphics: GuiGraphics) = graphics.setColor(1f, 1f, 1f, 1f)

    private fun withAlpha(color: Int, alpha: Float) = SaoUiStyle.multiplyAlpha(color, alpha)

    companion object {
        private val LEGACY_GUI = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("saoui", "textures/guiedt.png")

        private fun cubicBezier(x: Float, x1: Double, y1: Double, x2: Double, y2: Double): Double {
            var low = 0.0
            var high = 1.0
            repeat(16) {
                val t = (low + high) * 0.5
                if (bezier(t, x1, x2) < x) low = t else high = t
            }
            return bezier((low + high) * 0.5, y1, y2)
        }

        private fun bezier(t: Double, p1: Double, p2: Double): Double {
            val inverse = 1.0 - t
            return 3.0 * inverse * inverse * t * p1 + 3.0 * inverse * t * t * p2 + t * t * t
        }
    }
}

data class PopupButton(
    val icon: SaoIcon,
    val color: Int,
    val hoverColor: Int,
    val label: String? = null,
    val closeOnClick: Boolean = true,
    val action: (() -> Unit)?,
) {
    constructor(icon: SaoIcon, color: Int, hoverColor: Int, action: (() -> Unit)?) :
        this(icon, color, hoverColor, null, true, action)
}
