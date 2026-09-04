/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import net.fabricmc.loader.api.FabricLoader

object MCUIThemes {
    val DEVELOPMENT_THEME_ID = ThemeId("mcui", "development_test")
    val DEFAULT_THEME_ID = ThemeId("mcui", "saoui_reborn")

    val manager = ThemeManager(
        preferredTheme = if (FabricLoader.getInstance().isDevelopmentEnvironment) {
            DEVELOPMENT_THEME_ID
        } else {
            DEFAULT_THEME_ID
        },
    )
}
