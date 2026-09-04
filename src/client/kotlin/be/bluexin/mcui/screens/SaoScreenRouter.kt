/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import be.bluexin.mcui.mixin.client.DeathScreenAccessor
import net.minecraft.client.gui.screens.DeathScreen
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/** Converts only exact vanilla entry screens, leaving other mods' subclasses intact. */
object SaoScreenRouter {
    @JvmStatic
    fun route(screen: Screen?): Screen? = when {
        screen == null -> null
        screen.javaClass == TitleScreen::class.java -> SaoTitleScreen()
        screen.javaClass == PauseScreen::class.java && (screen as PauseScreen).showsPauseMenu() -> SaoIngameMenuScreen()
        screen.javaClass == InventoryScreen::class.java -> SaoInventoryScreen()
        screen.javaClass == DeathScreen::class.java -> {
            val accessor = screen as DeathScreenAccessor
            SaoDeathScreen(accessor.mcuiCauseOfDeath, accessor.mcuiHardcore)
        }
        else -> screen
    }
}
