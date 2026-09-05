/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * Both historical custom inventory implementations stop at an empty shell.
 * Keep the modern inventory's complete, native-size layout intact instead of
 * stretching menu-label atlases over container slots or inventing a new layout.
 */
class SaoInventoryScreen : InventoryScreen(
    requireNotNull(Minecraft.getInstance().player) { "SAO inventory requires a local player" },
), SaoScreenSurface
