/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.screens

import be.bluexin.mcui.util.legacyMcuiId
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/** Shared, immediate-mode SAO screen styling. No animation state is retained. */
object SaoUiStyle {
    const val TEXT = 0xFF555555.toInt()
    const val MUTED_TEXT = 0xFF888888.toInt()
    const val LIGHT_TEXT = 0xFFFFFFFF.toInt()
    const val GOLD = 0xFFC99B13.toInt()
    const val GOLD_LIGHT = 0xFFFFD76A.toInt()
    const val DISABLED = 0xFF7C7C7C.toInt()
    const val PANEL = 0xE8FFFFFF.toInt()
    const val PANEL_DARK = 0xD92B3038.toInt()
    const val SHADOW = 0x99000000.toInt()

    @JvmField
    val LOGO: ResourceLocation = legacyMcuiId("textures/logo.png")

    @JvmField
    val PROFILE_BACKGROUND: ResourceLocation = legacyMcuiId("textures/menu/parts/profilebg.png")

    @JvmField
    val ALERT_BACKGROUND: ResourceLocation = legacyMcuiId("textures/menu/parts/alertbg.png")

    @JvmField
    val SLOT: ResourceLocation = legacyMcuiId("textures/slot.png")

    @JvmField
    val DEATH: ResourceLocation = legacyMcuiId("textures/hud/buttons/death.png")

    @JvmStatic
    fun renderMenuBackground(graphics: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int) {
        graphics.fillGradient(left, top, right, bottom, 0xF02B3038.toInt(), 0xF015181E.toInt())
        val center = (left + right) / 2
        graphics.fill(left, top, center, bottom, 0x122DD8C7)
        graphics.fill(center, top, right, bottom, 0x0CC99B13)
    }

    @JvmStatic
    fun renderInWorldBackground(graphics: GuiGraphics, width: Int, height: Int) {
        graphics.fill(0, 0, width, height, 0x66000000)
        graphics.fillGradient(0, 0, width, height, 0x182DD8C7, 0x1815181E)
    }

    @JvmStatic
    fun renderPanel(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, dark: Boolean = false) {
        if (width <= 0 || height <= 0) return
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, SHADOW)
        graphics.fill(x, y, x + width, y + height, if (dark) PANEL_DARK else PANEL)
        graphics.fill(x, y, x + 3, y + height, GOLD)
        graphics.fill(x + 3, y, x + width, y + 1, 0x99FFFFFF.toInt())
        graphics.fill(x + 3, y + height - 1, x + width, y + height, 0x88555555.toInt())
    }

    @JvmStatic
    fun renderButton(
        graphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        message: Component,
        hovered: Boolean,
        active: Boolean,
        alpha: Float = 1f,
        icon: SaoIcon? = null,
        compact: Boolean = false,
    ) {
        val base = when {
            !active -> DISABLED
            hovered -> GOLD
            else -> PANEL
        }
        val color = withAlpha(base, alpha)
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, withAlpha(SHADOW, alpha))
        graphics.fill(x, y, x + width, y + height, color)
        graphics.fill(x, y, x + 2, y + height, withAlpha(if (hovered) GOLD_LIGHT else GOLD, alpha))
        graphics.fill(x + 2, y, x + width, y + 1, withAlpha(0xCCFFFFFF.toInt(), alpha))
        graphics.fill(x + 2, y + height - 1, x + width, y + height, withAlpha(0x88555555.toInt(), alpha))

        icon?.let {
            val size = minOf(16, height - 4)
            renderIcon(graphics, it, x + (if (compact) (width - size) / 2 else 5), y + (height - size) / 2, size)
        }

        if (!compact) {
            val font = Minecraft.getInstance().font
            val textColor = withAlpha(if (active && hovered) LIGHT_TEXT else if (active) TEXT else LIGHT_TEXT, alpha)
            val textX = if (icon == null) x + width / 2 else x + 26
            val centered = icon == null
            val drawX = if (centered) textX - font.width(message) / 2 else textX
            graphics.drawString(font, message, drawX, y + (height - 8) / 2, textColor, true)
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
        active: Boolean,
        alpha: Float,
        value: Double,
    ) {
        renderButton(graphics, x, y, width, height, message, hovered, active, alpha)
        val trackLeft = x + 5
        val trackRight = x + width - 5
        val trackY = y + height - 4
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2, withAlpha(0xFF555555.toInt(), alpha))
        val knobX = trackLeft + ((trackRight - trackLeft - 4) * value.coerceIn(0.0, 1.0)).toInt()
        graphics.fill(knobX, trackY - 2, knobX + 4, trackY + 4, withAlpha(if (hovered) GOLD_LIGHT else GOLD, alpha))
    }

    @JvmStatic
    fun renderEditBoxChrome(graphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int, focused: Boolean) {
        val border = if (focused) GOLD_LIGHT else GOLD
        graphics.fill(x - 1, y - 1, x + width + 1, y, border)
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, border)
        graphics.fill(x - 1, y, x, y + height, border)
        graphics.fill(x + width, y, x + width + 1, y + height, border)
    }

    @JvmStatic
    fun renderContainerChrome(graphics: GuiGraphics, left: Int, top: Int, width: Int, height: Int) {
        graphics.fill(left - 3, top - 3, left + width + 3, top - 2, GOLD)
        graphics.fill(left - 3, top + height + 2, left + width + 3, top + height + 3, GOLD)
        graphics.fill(left - 3, top - 2, left - 2, top + height + 2, GOLD)
        graphics.fill(left + width + 2, top - 2, left + width + 3, top + height + 2, GOLD)
    }

    fun renderIcon(graphics: GuiGraphics, icon: SaoIcon, x: Int, y: Int, size: Int = 16) {
        graphics.blit(icon.texture, x, y, size, size, 0f, 0f, 64, 64, 64, 64)
    }

    fun renderSlot(graphics: GuiGraphics, x: Int, y: Int, size: Int = 18) {
        graphics.blit(SLOT, x, y, size, size, 0f, 0f, 256, 256, 256, 256)
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val original = color ushr 24 and 0xFF
        val result = (original * alpha.coerceIn(0f, 1f)).toInt()
        return color and 0x00FFFFFF or (result shl 24)
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
