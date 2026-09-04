/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.themes.HudPartType
import be.bluexin.mcui.themes.MCUIThemes

/** Per-part replacement policy. A phase-three root alone never suppresses vanilla HUD. */
object VanillaHudPolicy {
    @JvmStatic
    fun suppresses(part: HudPartType): Boolean = MCUIThemes.manager.activeTheme.hud.provides(part)
}
