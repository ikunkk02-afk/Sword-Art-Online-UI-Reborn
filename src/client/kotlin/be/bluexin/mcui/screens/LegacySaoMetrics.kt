/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

/**
 * Values in this object have a direct origin/1.16.5 source. Compatibility-only
 * dimensions must not be added here.
 */
object LegacySaoMetrics {
    const val LEGACY_ATLAS_SIZE = 256

    // Original source: api/elements/IconElement.kt.
    const val ICON_SIZE = 19
    const val ICON_BOUND = 20
    const val ICON_CONTENT_SIZE = 16
    const val ICON_CONTENT_OFFSET = 1
    const val ICON_BACKGROUND_U = 1
    const val ICON_BACKGROUND_V = 26
    const val CHILD_X_OFFSET = 25
    const val CHILD_Y_SPACING = 20
    const val MAX_VISIBLE_CHILDREN = 7
    const val UNFOCUSED_ALPHA = 0.5f
    const val CAN_DRAW_ALPHA = 0.03f

    // Original source: screens/menus/IngameMenu.kt.
    const val ROOT_CENTER_X_OFFSET = 10
    const val TOP_LEVEL_SPACING = 25

    // Original source: api/elements/IconLabelElement.kt.
    const val LABEL_MIN_WIDTH = 84
    const val LABEL_HEIGHT = 18
    const val LABEL_TEXT_X = 22
    const val LABEL_WIDTH_TEXT_PADDING = 26
    const val LABEL_CHILD_GAP = 5
    const val LABEL_BACKGROUND_U = 0
    const val LABEL_BACKGROUND_V = 40

    // Original source: api/elements/ProfileElement.kt.
    const val PROFILE_X = -190
    const val PROFILE_Y = -153
    const val PROFILE_WIDTH = 165
    const val PROFILE_HEIGHT = 256
    const val PROFILE_ENTITY_SIZE = 40
    const val PROFILE_ENTITY_CENTER_X_OFFSET = -10
    const val PROFILE_NAME_X = 50
    const val PROFILE_NAME_Y = 20
    const val PROFILE_STATS_Y = 180
    const val PROFILE_SHADOW_U = 200
    const val PROFILE_SHADOW_V = 85
    const val PROFILE_SHADOW_SOURCE_WIDTH = 56
    const val PROFILE_SHADOW_SOURCE_HEIGHT = 30

    // Original source: screens/util/Popup.kt.
    const val POPUP_WIDTH = 220
    const val POPUP_BASE_HEIGHT = 160
    const val POPUP_TITLE_HEIGHT = 40
    const val POPUP_OPEN_HEIGHT_LOSS = 40
    const val POPUP_LINE_HEIGHT_GAIN = 10
    const val POPUP_SHADOW_HEIGHT = 20
    const val POPUP_OPEN_SHADOW_HEIGHT = 30
    const val POPUP_SHADOW_SWITCH = 0.66f
    const val POPUP_TEXT_EXPANSION_HEIGHT = 20
    const val POPUP_TEXT_OPEN_LOSS = 60
    const val POPUP_BUTTON_BAND_HEIGHT = 60
    const val POPUP_TITLE_SOURCE_V = 0
    const val POPUP_TITLE_SOURCE_HEIGHT = 64
    const val POPUP_TOP_SHADOW_SOURCE_V = 64
    const val POPUP_TEXT_SOURCE_V = 96
    const val POPUP_BOTTOM_SHADOW_SOURCE_V = 128
    const val POPUP_SHADOW_SOURCE_HEIGHT = 32
    const val POPUP_BUTTON_SOURCE_V = 160
    const val POPUP_BUTTON_SOURCE_HEIGHT = 96
    const val POPUP_BUTTON_HALF_SIZE = 9
    const val POPUP_BUTTON_INITIAL_Y = 11
    const val POPUP_BUTTON_DESTINATION_Y = 21
    const val POPUP_BUTTON_LINE_Y = 5
    const val POPUP_EARLY_SCALE_THRESHOLD = 0.2f
    const val POPUP_EARLY_SCALE_MULTIPLIER = 4f
    const val POPUP_TEXT_VISIBLE_ALPHA = 0.56f
    const val POPUP_TEXT_FADE_OFFSET = 0.5f
    const val POPUP_EASING_X1 = 0.755
    const val POPUP_EASING_Y1 = 0.05
    const val POPUP_EASING_X2 = 0.855
    const val POPUP_EASING_Y2 = 0.06

    // Original source: screens/ingame/DeathGui.kt.
    const val DEATH_WIDTH = 280
    const val DEATH_HEIGHT = 100
    const val DEATH_FADE_TICKS = 40

    /*
     * Animator.tick handled both Forge client tick phases in 1.16.5, so the
     * observed runtime advanced at approximately 40 units/s. These wall-clock
     * values preserve that observed speed on the modern monotonic clock.
     */
    const val CHILD_REVEAL_MILLIS = 75L // IndexedScheduledCounter(3f)
    const val LABEL_FADE_MILLIS = 100L // opacity duration=4f
    const val MOVE_MILLIS = 250L // position duration=10f
    const val POPUP_OPEN_MILLIS = 500L // duration=20f
    const val POPUP_CLOSE_MILLIS = 250L // duration=10f

    // Original source: screens/CoreGui.kt.
    const val ROOT_MOUSE_MOVEMENT = 0.25
    const val PLAYER_VIEW_MOUSE_MOVEMENT = 0.125f
    const val MOVE_EASING_X1 = 0.86
    const val MOVE_EASING_Y1 = 0.0
    const val MOVE_EASING_X2 = 0.07
    const val MOVE_EASING_Y2 = 1.0

    // Original source: themes/sao/style.css, converted RRGGBBAA -> AARRGGBB.
    const val DEFAULT_BACKGROUND = -0x1 // #FFFFFFFF
    const val DEFAULT_TEXT = -0x777778 // #FF888888
    const val HOVER_BACKGROUND = -0x3664ed // #FFC99B13
    const val DISABLED_BACKGROUND = -0x838384 // #FF7C7C7C
    const val WHITE = -0x1
    const val CONFIRM = -0xb87d1d // #FF4782E3
    const val CONFIRM_HOVER = -0x9d6201 // #FF629DFF
    const val CANCEL = -0x1cb8b9 // #FFE34747
    const val CANCEL_HOVER = -0x9d9e // #FFFF6262
    const val POPUP = -0x444445 // #FFBBBBBB
    const val POPUP_TEXT = -0xaaaaab // #FF555555
    const val DEATH = -0x36bebf // #FFC94141
    const val HARDCORE_DEATH = -0x670000 // #FF990000
}
