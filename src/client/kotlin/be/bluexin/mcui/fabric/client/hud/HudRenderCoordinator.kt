/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.animation.AnimationRegistry
import be.bluexin.mcui.render.MinecraftGuiRenderOperations
import be.bluexin.mcui.render.RenderContext
import be.bluexin.mcui.render.RenderingElementVisitor
import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.TargetEntityHealthElement
import be.bluexin.mcui.themes.HudPartType
import be.bluexin.mcui.themes.ResolvedHud
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/** Selects visible HUD parts and delegates resolved elements to the existing renderer. */
class HudRenderCoordinator {
    private val animations = AnimationRegistry()
    private var cachedThemeRevision = Long.MIN_VALUE
    private var cachedThemeId = ""
    private var cachedTargetLinger: Int? = null

    fun render(
        themeRevision: Long,
        themeId: String,
        hud: ResolvedHud,
        data: HudDataSnapshot,
        graphics: GuiGraphics,
        minecraft: Minecraft,
    ) {
        animations.beginFrame(themeRevision, themeId, data.targetEntity, data.nearbyEntities)
        if (cachedThemeRevision != themeRevision || cachedThemeId != themeId) {
            cachedThemeRevision = themeRevision
            cachedThemeId = themeId
            cachedTargetLinger = hud[HudPartType.ENTITY_HEALTH_HUD]?.let(::targetLinger)
        }
        val context = RenderContext(
            partialTick = data.partialTick,
            guiWidth = data.guiWidth,
            guiHeight = data.guiHeight,
        )
        MinecraftGuiRenderOperations(graphics, minecraft).use { operations ->
            val visitor = RenderingElementVisitor(operations, data, animations)
            hud.globalOverlay?.let { visitor.render(it, context) }
            RENDER_ORDER.forEach { part ->
                val root = hud[part] ?: return@forEach
                val targetLinger = cachedTargetLinger
                val lingeringTarget = if (part == HudPartType.ENTITY_HEALTH_HUD) {
                    animations.targetEntity(targetLinger ?: DEFAULT_TARGET_LINGER_MILLIS)
                } else null
                val targetVisible = HudPartVisibility.isVisible(part, data) ||
                    (targetLinger != null && lingeringTarget != null)
                if (animations.shouldRenderPart(part, root, targetVisible)) {
                    visitor.render(root, context.copy(visibilityTarget = targetVisible))
                }
            }
        }
    }

    private fun targetLinger(element: Element): Int? = when (element) {
        is TargetEntityHealthElement -> element.lingerMillis
        is GroupElement -> element.children.firstNotNullOfOrNull(::targetLinger)
        else -> null
    }

    private companion object {
        const val DEFAULT_TARGET_LINGER_MILLIS = 3000
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
