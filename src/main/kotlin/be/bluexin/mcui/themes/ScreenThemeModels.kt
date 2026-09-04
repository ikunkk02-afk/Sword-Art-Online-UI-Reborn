/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.themes

import kotlinx.serialization.Serializable

/**
 * Visual tokens shared by screens and widgets. Layout remains Kotlin-owned so this
 * resource cannot turn into a GUI scripting language.
 */
@Serializable
data class ScreenThemeDefinition(
    val version: String = "1",
    val colors: ScreenColorDefinition = ScreenColorDefinition(),
    val textures: ScreenTextureDefinition = ScreenTextureDefinition(),
    val opacity: ScreenOpacityDefinition = ScreenOpacityDefinition(),
    val spacing: ScreenSpacingDefinition = ScreenSpacingDefinition(),
    val animation: ScreenAnimationDefinition = ScreenAnimationDefinition(),
)

@Serializable
data class ScreenColorDefinition(
    val background: ArgbColorDefinition = argb(0xF02B3038),
    val backgroundSecondary: ArgbColorDefinition = argb(0xF015181E),
    val worldOverlay: ArgbColorDefinition = argb(0x66000000),
    val worldOverlayAccent: ArgbColorDefinition = argb(0x182DD8C7),
    val panel: ArgbColorDefinition = argb(0xE8FFFFFF),
    val panelDark: ArgbColorDefinition = argb(0xD92B3038),
    val panelHighlight: ArgbColorDefinition = argb(0x99FFFFFF),
    val panelShadow: ArgbColorDefinition = argb(0x99000000),
    val button: ArgbColorDefinition = argb(0xE8FFFFFF),
    val buttonHover: ArgbColorDefinition = argb(0xFFC99B13),
    val buttonFocused: ArgbColorDefinition = argb(0xFFFFD76A),
    val buttonPressed: ArgbColorDefinition = argb(0xFF9E7910),
    val buttonDisabled: ArgbColorDefinition = argb(0xFF7C7C7C),
    val selected: ArgbColorDefinition = argb(0xFFC99B13),
    val accent: ArgbColorDefinition = argb(0xFFC99B13),
    val accentLight: ArgbColorDefinition = argb(0xFFFFD76A),
    val text: ArgbColorDefinition = argb(0xFF555555),
    val textHover: ArgbColorDefinition = argb(0xFFFFFFFF),
    val textDisabled: ArgbColorDefinition = argb(0xFFFFFFFF),
    val mutedText: ArgbColorDefinition = argb(0xFF888888),
    val lightText: ArgbColorDefinition = argb(0xFFFFFFFF),
    val slot: ArgbColorDefinition = argb(0x99FFFFFF),
    val slotEquipment: ArgbColorDefinition = argb(0x66C99B13),
    val dialog: ArgbColorDefinition = argb(0xF4FFFFFF),
    val title: ArgbColorDefinition = argb(0xFFC99B13),
    val icon: ArgbColorDefinition = argb(0xFFFFFFFF),
    val iconDisabled: ArgbColorDefinition = argb(0xB0FFFFFF),
    val sliderTrack: ArgbColorDefinition = argb(0xFF555555),
    val editBoxBackground: ArgbColorDefinition = argb(0xD915181E),
)

@Serializable
data class ScreenTextureDefinition(
    val menuBackground: String? = null,
    val logo: String = "saoui:textures/logo.png",
    val profileBackground: String = "saoui:textures/menu/parts/profilebg.png",
    val dialogBackground: String = "saoui:textures/menu/parts/alertbg.png",
    val slot: String = "saoui:textures/slot.png",
    val death: String = "saoui:textures/hud/buttons/death.png",
)

@Serializable
data class ScreenOpacityDefinition(
    val menuBackground: Double = 1.0,
    val worldOverlay: Double = 1.0,
    val panel: Double = 1.0,
    val disabled: Double = 0.72,
    val icon: Double = 1.0,
)

@Serializable
data class ScreenSpacingDefinition(
    val padding: Int = 8,
    val margin: Int = 12,
    val border: Int = 2,
    val buttonGap: Int = 4,
    val slotInset: Int = 1,
)

@Serializable
data class ScreenAnimationDefinition(
    val screenFadeMillis: Int = 180,
    val panelSlidePixels: Int = 8,
    val categoryMillis: Int = 160,
    val buttonStaggerMillis: Int = 15,
)

private fun argb(value: Long): ArgbColorDefinition = ArgbColorDefinition(value.toInt())
