/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * The active 1.16.5 InventoryGui/ContainerElements implementation is commented
 * out and supplies no authoritative layout. Keep every modern container action
 * vanilla-owned until original visual evidence exists instead of inventing a
 * profile/equipment panel composition.
 */
class SaoInventoryScreen : InventoryScreen(
    requireNotNull(Minecraft.getInstance().player) { "SAO inventory requires a local player" },
), SaoScreenSurface
