/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

/** Marker used by routing and skin mixins without matching arbitrary mod subclasses. */
interface SaoScreenSurface

/** Exact ownership rules for global-looking screen mixins. */
object SaoScreenPolicy {
    @JvmStatic
    fun shouldSkinWidgets(screen: Screen?): Boolean =
        screen !is SaoInventoryScreen && screen is SaoScreenSurface

    @JvmStatic
    fun shouldReplaceMenuBackground(screen: Screen?): Boolean =
        Minecraft.getInstance().level == null && shouldSkinWidgets(screen)

    @JvmStatic
    fun shouldRenderContainerFrame(screen: Screen?): Boolean =
        screen !is SaoInventoryScreen &&
            screen is AbstractContainerScreen<*> &&
            screen is SaoScreenSurface

    @JvmStatic
    fun shouldRenderContainerSlots(screen: Screen?): Boolean =
        shouldRenderContainerFrame(screen)
}
