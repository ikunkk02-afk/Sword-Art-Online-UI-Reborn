/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.render.MinecraftGuiRenderOperations
import be.bluexin.mcui.render.RenderContext
import be.bluexin.mcui.render.RenderingElementVisitor
import be.bluexin.mcui.themes.HudPartType
import be.bluexin.mcui.themes.ResolvedHud
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/** Selects visible HUD parts and delegates resolved elements to the existing renderer. */
class HudRenderCoordinator {
    fun render(hud: ResolvedHud, data: HudDataSnapshot, graphics: GuiGraphics, minecraft: Minecraft) {
        val context = RenderContext(
            partialTick = data.partialTick,
            guiWidth = data.guiWidth,
            guiHeight = data.guiHeight,
        )
        MinecraftGuiRenderOperations(graphics, minecraft).use { operations ->
            val visitor = RenderingElementVisitor(operations, data)
            hud.globalOverlay?.let { visitor.render(it, context) }
            RENDER_ORDER.forEach { part ->
                if (HudPartVisibility.isVisible(part, data)) {
                    hud[part]?.let { visitor.render(it, context) }
                }
            }
        }
    }

    private companion object {
        val RENDER_ORDER = listOf(
            HudPartType.HEALTH_BOX,
            HudPartType.ARMOR,
            HudPartType.FOOD,
            HudPartType.AIR,
            HudPartType.MOUNT_HEALTH,
            HudPartType.JUMP_BAR,
            HudPartType.EXPERIENCE,
            HudPartType.HOTBAR,
            HudPartType.EFFECTS,
            HudPartType.AM2BARS,
            HudPartType.PARTY,
            HudPartType.ENTITY_HEALTH_HUD,
            HudPartType.CROSS_HAIR,
        )
    }
}
