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
    /**
     * The original SETTINGS -> MENU entry deliberately opened Minecraft's pause menu.
     * Keep that one concrete instance outside the exact-class replacement without
     * weakening routing for later vanilla screens.
     */
    private var legacyMenuPassthrough: Screen? = null

    @JvmStatic
    fun openVanillaPause(minecraft: net.minecraft.client.Minecraft) {
        val screen = PauseScreen(true)
        legacyMenuPassthrough = screen
        minecraft.setScreen(screen)
    }

    @JvmStatic
    fun route(screen: Screen?): Screen? {
        if (screen === legacyMenuPassthrough) return screen
        legacyMenuPassthrough = null
        return when {
            screen == null -> null
            screen is SaoScreenSurface -> screen
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
}
