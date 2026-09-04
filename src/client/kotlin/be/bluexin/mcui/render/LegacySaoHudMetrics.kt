/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.render

/**
 * Literal reference values from origin/1.16.5
 * `assets/saoui/themes/sao/hud.xml`, `StatusEffects.kt`, and its CSS colors.
 */
internal object LegacySaoHudMetrics {
    const val ATLAS_SIZE = 256
    const val USERNAME_WIDTH_PADDING = 9

    const val FRAME_HEIGHT = 15
    const val FRAME_LEFT_WIDTH = 16
    const val FRAME_STRETCH_U = 16
    const val FRAME_STRETCH_SOURCE_WIDTH = 5
    const val FRAME_TAIL_U = 21
    const val FRAME_TAIL_WIDTH = 234
    const val USERNAME_X = 16
    const val USERNAME_Y = 4

    const val PLAYER_BAR_X = 18
    const val HEALTH_BAR_Y = 3
    const val PLAYER_BAR_MAX_WIDTH = 215
    const val PLAYER_BAR_HEIGHT = 9
    const val PLAYER_BAR_U = 0
    const val PLAYER_BAR_V = 188

    const val FOOD_BAR_Y = 10
    const val FOOD_BAR_MAX_WIDTH = 113
    const val FOOD_BAR_SOURCE_WIDTH = 115
    const val FOOD_BAR_HEIGHT = 2
    const val FOOD_BAR_V = 193

    const val HP_PANEL_BASE_X = 132
    const val LEVEL_PANEL_BASE_X = 142
    const val VALUE_PANEL_Y = 12
    const val VALUE_TEXT_X = 5
    const val VALUE_TEXT_Y = 15
    const val VALUE_PANEL_HEIGHT = 13
    const val VALUE_CAP_WIDTH = 5
    const val HP_LEFT_U = 60
    const val HP_MIDDLE_U = 66
    const val HP_MIDDLE_SOURCE_WIDTH = 5
    const val HP_RIGHT_U = 70
    const val LEVEL_LEFT_U = 65
    const val LEVEL_LEFT_SOURCE_WIDTH = 2
    const val LEVEL_MIDDLE_U = 66
    const val LEVEL_MIDDLE_SOURCE_WIDTH = 5
    const val LEVEL_RIGHT_U = 78
    const val LEVEL_RIGHT_SOURCE_WIDTH = 3
    const val VALUE_PANEL_V = 15

    const val EFFECTS_BASE_X = 248
    const val EFFECTS_Y = 2
    const val EFFECT_ICON_SIZE = 16
    const val EFFECT_X_STEP = 11
    const val EFFECT_TEXTURE_SIZE = 16

    const val ENTITY_MAX_ROWS = 5
    const val ENTITY_ROW_HEIGHT = 15
    const val ENTITY_FILL_X = 1
    const val ENTITY_FILL_Y = 1
    const val ENTITY_FILL_HALF_PIXEL_Y = 0.5f
    const val ENTITY_FILL_MAX_WIDTH = 79
    const val ENTITY_FILL_HEIGHT = 14
    const val ENTITY_FILL_U = 1
    const val ENTITY_FILL_V = 0
    const val ENTITY_FRAME_WIDTH = 80
    const val ENTITY_FRAME_HEIGHT = 15
    const val ENTITY_FRAME_U = 1
    const val ENTITY_FRAME_V = 30
    const val ENTITY_SOURCE_WIDTH = 255
    const val ENTITY_SOURCE_HEIGHT = 30
    const val ENTITY_NAME_MAX_LENGTH = 20
    const val ENTITY_NAME_RIGHT_GAP = 5
    const val ENTITY_NAME_Y = 4

    const val HEALTH_VERY_LOW_THRESHOLD = 0.1f
    const val HEALTH_LOW_THRESHOLD = 0.2f
    const val HEALTH_VERY_DAMAGED_THRESHOLD = 0.3f
    const val HEALTH_DAMAGED_THRESHOLD = 0.4f
    const val HEALTH_OKAY_THRESHOLD = 0.5f

    val WHITE = ArgbColor(0xFFFFFFFF.toInt())
    val HP_VERY_LOW = ArgbColor(0xFFBD0000.toInt())
    val HP_LOW = ArgbColor(0xFFF40000.toInt())
    val HP_VERY_DAMAGED = ArgbColor(0xFFF47800.toInt())
    val HP_DAMAGED = ArgbColor(0xFFF4BD00.toInt())
    val HP_OKAY = ArgbColor(0xFFEDEB38.toInt())
    val HP_GOOD = ArgbColor(0xFF93F43E.toInt())
    val HP_CREATIVE = ArgbColor(0xFF4CEDC5.toInt())
    val AIR = ArgbColor(0x802ADDF5.toInt())
    val FOOD = ArgbColor(0xFFF5AB2A.toInt())
    val FOOD_ROTTEN = ArgbColor(0xFFD045FF.toInt())
}
