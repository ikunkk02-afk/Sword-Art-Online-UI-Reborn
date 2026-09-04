/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.themes

import net.minecraft.resources.ResourceLocation

data class ResolvedScreenTheme(
    val colors: ResolvedScreenColors,
    val textures: ResolvedScreenTextures,
    val opacity: ResolvedScreenOpacity,
    val spacing: ResolvedScreenSpacing,
    val animation: ResolvedScreenAnimation,
) {
    companion object {
        val FALLBACK: ResolvedScreenTheme = fromDefinition(ScreenThemeDefinition())

        internal fun fromDefinition(definition: ScreenThemeDefinition): ResolvedScreenTheme = ResolvedScreenTheme(
            colors = ResolvedScreenColors.from(definition.colors),
            textures = ResolvedScreenTextures(
                menuBackground = definition.textures.menuBackground?.let(ResourceLocation::tryParse),
                logo = requiredTexture(definition.textures.logo, "saoui:textures/logo.png"),
                profileBackground = requiredTexture(
                    definition.textures.profileBackground,
                    "saoui:textures/menu/parts/profilebg.png",
                ),
                dialogBackground = requiredTexture(
                    definition.textures.dialogBackground,
                    "saoui:textures/menu/parts/alertbg.png",
                ),
                slot = requiredTexture(definition.textures.slot, "saoui:textures/slot.png"),
                death = requiredTexture(definition.textures.death, "saoui:textures/hud/buttons/death.png"),
            ),
            opacity = ResolvedScreenOpacity(
                menuBackground = definition.opacity.menuBackground.toFloat().coerceIn(0f, 1f),
                worldOverlay = definition.opacity.worldOverlay.toFloat().coerceIn(0f, 1f),
                panel = definition.opacity.panel.toFloat().coerceIn(0f, 1f),
                disabled = definition.opacity.disabled.toFloat().coerceIn(0f, 1f),
                icon = definition.opacity.icon.toFloat().coerceIn(0f, 1f),
            ),
            spacing = ResolvedScreenSpacing(
                padding = definition.spacing.padding.coerceIn(0, 64),
                margin = definition.spacing.margin.coerceIn(0, 64),
                border = definition.spacing.border.coerceIn(1, 8),
                buttonGap = definition.spacing.buttonGap.coerceIn(0, 16),
                slotInset = definition.spacing.slotInset.coerceIn(0, 4),
            ),
            animation = ResolvedScreenAnimation(
                screenFadeMillis = definition.animation.screenFadeMillis.coerceIn(0, 250),
                panelSlidePixels = definition.animation.panelSlidePixels.coerceIn(0, 24),
                categoryMillis = definition.animation.categoryMillis.coerceIn(0, 250),
                buttonStaggerMillis = definition.animation.buttonStaggerMillis.coerceIn(0, 50),
            ),
        )

        private fun requiredTexture(value: String, fallback: String): ResourceLocation =
            ResourceLocation.tryParse(value) ?: checkNotNull(ResourceLocation.tryParse(fallback))
    }
}

data class ResolvedScreenColors(
    val background: Int,
    val backgroundSecondary: Int,
    val worldOverlay: Int,
    val worldOverlayAccent: Int,
    val panel: Int,
    val panelDark: Int,
    val panelHighlight: Int,
    val panelShadow: Int,
    val button: Int,
    val buttonHover: Int,
    val buttonFocused: Int,
    val buttonPressed: Int,
    val buttonDisabled: Int,
    val selected: Int,
    val accent: Int,
    val accentLight: Int,
    val text: Int,
    val textHover: Int,
    val textDisabled: Int,
    val mutedText: Int,
    val lightText: Int,
    val slot: Int,
    val slotEquipment: Int,
    val dialog: Int,
    val title: Int,
    val icon: Int,
    val iconDisabled: Int,
    val sliderTrack: Int,
    val editBoxBackground: Int,
) {
    companion object {
        fun from(value: ScreenColorDefinition) = ResolvedScreenColors(
            value.background.value,
            value.backgroundSecondary.value,
            value.worldOverlay.value,
            value.worldOverlayAccent.value,
            value.panel.value,
            value.panelDark.value,
            value.panelHighlight.value,
            value.panelShadow.value,
            value.button.value,
            value.buttonHover.value,
            value.buttonFocused.value,
            value.buttonPressed.value,
            value.buttonDisabled.value,
            value.selected.value,
            value.accent.value,
            value.accentLight.value,
            value.text.value,
            value.textHover.value,
            value.textDisabled.value,
            value.mutedText.value,
            value.lightText.value,
            value.slot.value,
            value.slotEquipment.value,
            value.dialog.value,
            value.title.value,
            value.icon.value,
            value.iconDisabled.value,
            value.sliderTrack.value,
            value.editBoxBackground.value,
        )
    }
}

data class ResolvedScreenTextures(
    val menuBackground: ResourceLocation?,
    val logo: ResourceLocation,
    val profileBackground: ResourceLocation,
    val dialogBackground: ResourceLocation,
    val slot: ResourceLocation,
    val death: ResourceLocation,
)

data class ResolvedScreenOpacity(
    val menuBackground: Float,
    val worldOverlay: Float,
    val panel: Float,
    val disabled: Float,
    val icon: Float,
)

data class ResolvedScreenSpacing(
    val padding: Int,
    val margin: Int,
    val border: Int,
    val buttonGap: Int,
    val slotInset: Int,
)

data class ResolvedScreenAnimation(
    val screenFadeMillis: Int,
    val panelSlidePixels: Int,
    val categoryMillis: Int,
    val buttonStaggerMillis: Int,
)
