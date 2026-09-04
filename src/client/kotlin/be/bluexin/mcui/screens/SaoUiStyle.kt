/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import be.bluexin.mcui.themes.MCUIThemes
import be.bluexin.mcui.themes.ResolvedScreenTheme
import be.bluexin.mcui.util.legacyMcuiId
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/** Immediate-mode render helpers backed by the immutable active screen-theme snapshot. */
object SaoUiStyle {
    private val style: ResolvedScreenTheme
        get() = MCUIThemes.manager.activeTheme.screens

    @get:JvmStatic val TEXT: Int get() = style.colors.text
    @get:JvmStatic val MUTED_TEXT: Int get() = style.colors.mutedText
    @get:JvmStatic val LIGHT_TEXT: Int get() = style.colors.lightText
    @get:JvmStatic val GOLD: Int get() = style.colors.accent
    @get:JvmStatic val GOLD_LIGHT: Int get() = style.colors.accentLight
    @get:JvmStatic val DISABLED: Int get() = style.colors.buttonDisabled
    @get:JvmStatic val PANEL: Int get() = style.colors.panel
    @get:JvmStatic val PANEL_DARK: Int get() = style.colors.panelDark
    @get:JvmStatic val SHADOW: Int get() = style.colors.panelShadow
    @get:JvmStatic val TITLE: Int get() = style.colors.title

    val LOGO: ResourceLocation get() = style.textures.logo
    val PROFILE_BACKGROUND: ResourceLocation get() = style.textures.profileBackground
    val ALERT_BACKGROUND: ResourceLocation get() = style.textures.dialogBackground
    val SLOT: ResourceLocation get() = style.textures.slot
    val DEATH: ResourceLocation get() = style.textures.death

    @JvmStatic
    fun current(): ResolvedScreenTheme = style

    @JvmStatic
    fun renderMenuBackground(graphics: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int) {
        val colors = style.colors
        style.textures.menuBackground?.let { texture ->
            setColor(graphics, 0xFFFFFFFF.toInt(), style.opacity.menuBackground)
            graphics.blit(texture, left, top, right - left, bottom - top, 0f, 0f, 256, 256, 256, 256)
            resetColor(graphics)
            return
        }
        graphics.fillGradient(
            left,
            top,
            right,
            bottom,
            multiplyAlpha(colors.background, style.opacity.menuBackground),
            multiplyAlpha(colors.backgroundSecondary, style.opacity.menuBackground),
        )
        val center = (left + right) / 2
        graphics.fill(left, top, center, bottom, multiplyAlpha(colors.worldOverlayAccent, 0.75f))
        graphics.fill(center, top, right, bottom, multiplyAlpha(colors.accent, 0.05f))
    }

    @JvmStatic
    fun renderInWorldBackground(graphics: GuiGraphics, width: Int, height: Int) {
        val opacity = style.opacity.worldOverlay
        graphics.fill(0, 0, width, height, multiplyAlpha(style.colors.worldOverlay, opacity))
        graphics.fillGradient(
            0,
            0,
            width,
            height,
            multiplyAlpha(style.colors.worldOverlayAccent, opacity),
            multiplyAlpha(style.colors.backgroundSecondary, 0.1f * opacity),
        )
    }

    @JvmStatic
    @JvmOverloads
    fun renderPanel(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        dark: Boolean = false,
        alpha: Float = 1f,
    ) {
        if (width <= 0 || height <= 0) return
        val border = style.spacing.border
        val opacity = alpha * style.opacity.panel
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, multiplyAlpha(style.colors.panelShadow, opacity))
        graphics.fill(x, y, x + width, y + height, multiplyAlpha(if (dark) style.colors.panelDark else style.colors.panel, opacity))
        graphics.fill(x, y, x + border, y + height, multiplyAlpha(style.colors.accent, opacity))
        graphics.fill(x + border, y, x + width, y + 1, multiplyAlpha(style.colors.panelHighlight, opacity))
        graphics.fill(x + border, y + height - 1, x + width, y + height, multiplyAlpha(style.colors.text, 0.55f * opacity))
    }

    @JvmStatic
    @JvmOverloads
    fun renderButton(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        hovered: Boolean,
        focused: Boolean,
        pressed: Boolean,
        active: Boolean,
        selected: Boolean = false,
        alpha: Float = 1f,
        icon: SaoIcon? = null,
        compact: Boolean = false,
    ) {
        if (width <= 0 || height <= 0) return
        val colors = style.colors
        val effectiveAlpha = alpha.coerceIn(0f, 1f) * if (active) 1f else style.opacity.disabled
        val base = when {
            !active -> colors.buttonDisabled
            pressed -> colors.buttonPressed
            selected -> colors.selected
            hovered -> colors.buttonHover
            focused -> colors.buttonFocused
            else -> colors.button
        }
        val accent = if (focused || hovered || selected) colors.accentLight else colors.accent
        val border = style.spacing.border.coerceAtMost(width)
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, multiplyAlpha(colors.panelShadow, effectiveAlpha))
        graphics.fill(x, y, x + width, y + height, multiplyAlpha(base, effectiveAlpha))
        graphics.fill(x, y, x + border, y + height, multiplyAlpha(accent, effectiveAlpha))
        graphics.fill(x + border, y, x + width, y + 1, multiplyAlpha(colors.panelHighlight, effectiveAlpha))
        graphics.fill(x + border, y + height - 1, x + width, y + height, multiplyAlpha(colors.text, 0.55f * effectiveAlpha))

        icon?.let {
            val size = minOf(16, height - 4).coerceAtLeast(1)
            renderIcon(
                graphics,
                it,
                x + if (compact) (width - size) / 2 else 5,
                y + (height - size) / 2,
                size,
                if (active) colors.icon else colors.iconDisabled,
                effectiveAlpha * style.opacity.icon,
            )
        }

        if (!compact) {
            val font = Minecraft.getInstance().font
            val startX = if (icon == null) x + 5 else x + 26
            val available = (x + width - 5 - startX).coerceAtLeast(1)
            val fitted = fitText(message, available)
            val textColor = when {
                !active -> colors.textDisabled
                hovered || focused || pressed || selected -> colors.textHover
                else -> colors.text
            }
            val drawX = if (icon == null) x + (width - font.width(fitted)) / 2 else startX
            graphics.drawString(font, fitted, drawX, y + (height - 8) / 2, multiplyAlpha(textColor, effectiveAlpha), true)
        }
    }

    @JvmStatic
    fun renderSlider(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        hovered: Boolean,
        focused: Boolean,
        pressed: Boolean,
        active: Boolean,
        alpha: Float,
        value: Double,
    ) {
        renderButton(graphics, x, y, width, height, message, hovered, focused, pressed, active, alpha = alpha)
        val trackLeft = x + 6
        val trackRight = x + width - 6
        val trackY = y + height - 4
        val effectiveAlpha = alpha * if (active) 1f else style.opacity.disabled
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2, multiplyAlpha(style.colors.sliderTrack, effectiveAlpha))
        val knobX = trackLeft + ((trackRight - trackLeft - 5) * value.coerceIn(0.0, 1.0)).toInt()
        graphics.fill(
            knobX,
            trackY - 2,
            knobX + 5,
            trackY + 4,
            multiplyAlpha(if (hovered || focused) style.colors.accentLight else style.colors.accent, effectiveAlpha),
        )
    }

    @JvmStatic
    fun renderEditBoxChrome(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        hovered: Boolean,
        focused: Boolean,
        active: Boolean,
    ) {
        val border = when {
            !active -> style.colors.buttonDisabled
            focused -> style.colors.accentLight
            hovered -> style.colors.accent
            else -> style.colors.mutedText
        }
        val thickness = style.spacing.border
        graphics.fill(x - thickness, y - thickness, x + width + thickness, y, border)
        graphics.fill(x - thickness, y + height, x + width + thickness, y + height + thickness, border)
        graphics.fill(x - thickness, y, x, y + height, border)
        graphics.fill(x + width, y, x + width + thickness, y + height, border)
    }

    @JvmStatic
    fun renderEditBoxBackground(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, active: Boolean) {
        val color = if (active) style.colors.editBoxBackground else style.colors.buttonDisabled
        graphics.fill(x, y, x + width, y + height, multiplyAlpha(color, if (active) 1f else style.opacity.disabled))
    }

    @JvmStatic
    fun renderContainerChrome(graphics: GuiGraphics, left: Int, top: Int, width: Int, height: Int) {
        renderPanel(graphics, left - 4, top - 4, width + 8, height + 8, dark = true)
    }

    @JvmStatic
    @JvmOverloads
    fun renderIcon(
        graphics: GuiGraphics,
        icon: SaoIcon,
        x: Int,
        y: Int,
        size: Int = 16,
        color: Int = style.colors.icon,
        alpha: Float = 1f,
    ) {
        setColor(graphics, color, alpha)
        graphics.blit(icon.texture, x, y, size, size, 0f, 0f, 64, 64, 64, 64)
        resetColor(graphics)
    }

    @JvmStatic
    @JvmOverloads
    fun renderSlot(graphics: GuiGraphics, x: Int, y: Int, size: Int = 18, equipment: Boolean = false) {
        val inset = style.spacing.slotInset
        if (equipment) {
            graphics.fill(x + inset, y + inset, x + size - inset, y + size - inset, style.colors.slotEquipment)
        }
        graphics.blit(style.textures.slot, x, y, size, size, 0f, 0f, 256, 256, 256, 256)
    }

    @JvmStatic
    fun fitText(message: Component, maxWidth: Int): Component {
        val font = Minecraft.getInstance().font
        if (maxWidth <= 0 || font.width(message) <= maxWidth) return message
        val ellipsis = "…"
        val body = font.plainSubstrByWidth(message.string, (maxWidth - font.width(ellipsis)).coerceAtLeast(0))
        return Component.literal(body + ellipsis).withStyle(message.style)
    }

    @JvmStatic
    fun multiplyAlpha(color: Int, alpha: Float): Int {
        val original = color ushr 24 and 0xFF
        val result = (original * alpha.coerceIn(0f, 1f)).toInt()
        return color and 0x00FFFFFF or (result shl 24)
    }

    private fun setColor(graphics: GuiGraphics, color: Int, alpha: Float) {
        graphics.setColor(
            (color ushr 16 and 0xFF) / 255f,
            (color ushr 8 and 0xFF) / 255f,
            (color and 0xFF) / 255f,
            (color ushr 24 and 0xFF) / 255f * alpha.coerceIn(0f, 1f),
        )
    }

    private fun resetColor(graphics: GuiGraphics) {
        graphics.setColor(1f, 1f, 1f, 1f)
    }
}

enum class SaoIcon(path: String) {
    ACCESSORY("accessory"),
    ARMOR("armor"),
    CANCEL("cancel"),
    CONFIRM("confirm"),
    CRAFTING("crafting"),
    CREATE("create"),
    DUNGEON_MAP("dungeonmap"),
    EQUIPMENT("equipment"),
    FIELD_MAP("fieldmap"),
    FRIEND("friend"),
    GUILD("guild"),
    HELP("help"),
    INVITE("invite"),
    ITEMS("items"),
    LOGOUT("logout"),
    MESSAGE("message"),
    MESSAGE_RECEIVED("messagereceived"),
    NAVIGATION("navigation"),
    OPTION("option"),
    PARTY("party"),
    PROFILE("profile"),
    QUEST("quest"),
    SETTINGS("settings"),
    SKILLS("skills"),
    SNEAKING("sneaking"),
    SOCIAL("social"),
    SPRINTING("sprinting");

    val texture: ResourceLocation = legacyMcuiId("textures/menu/icons/$path.png")
}
