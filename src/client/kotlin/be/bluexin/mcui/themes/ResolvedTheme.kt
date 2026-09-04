/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import be.bluexin.mcui.render.element.Element
import be.bluexin.mcui.render.element.GroupElement

data class ResolvedHud(
    val globalOverlay: Element? = null,
    val parts: Map<HudPartType, Element> = emptyMap(),
) {
    operator fun get(part: HudPartType): Element? = parts[part]

    fun provides(part: HudPartType): Boolean = part in parts

    companion object {
        val EMPTY = ResolvedHud()
    }
}

data class ResolvedTheme(
    val id: ThemeId,
    val metadata: ThemeMetadata,
    val hud: ResolvedHud,
    val screens: ResolvedScreenTheme = ResolvedScreenTheme.FALLBACK,
    val sourcePack: String,
    val sourceResource: String,
    val elementCount: Int,
) {
    constructor(
        id: ThemeId,
        metadata: ThemeMetadata,
        hudRoot: Element,
        sourcePack: String,
        sourceResource: String,
        elementCount: Int,
    ) : this(
        id = id,
        metadata = metadata,
        hud = ResolvedHud(globalOverlay = hudRoot),
        screens = ResolvedScreenTheme.FALLBACK,
        sourcePack = sourcePack,
        sourceResource = sourceResource,
        elementCount = elementCount,
    )

    /** Source compatibility for phase-three callers; new HUD code consumes [hud]. */
    val hudRoot: Element
        get() = hud.globalOverlay ?: EMPTY_ROOT

    private companion object {
        val EMPTY_ROOT = GroupElement(children = emptyList())
    }
}

data class ThemeLoadResult(
    val themes: Map<ThemeId, ResolvedTheme>,
    val discoveredCount: Int,
    val failedCount: Int,
    val issues: List<ThemeIssue>,
    val fatalError: String? = null,
)

data class ThemeSnapshot(
    val revision: Long,
    val themes: Map<ThemeId, ResolvedTheme>,
    val activeTheme: ResolvedTheme,
) {
    companion object {
        val FALLBACK_THEME = ResolvedTheme(
            id = ThemeId("mcui", "empty"),
            metadata = ThemeMetadata(
                format = ThemeMetadata.RESOLVED_V1_FORMAT,
                version = "builtin",
                name = "Empty fallback",
                authors = listOf("MCUI"),
            ),
            hud = ResolvedHud.EMPTY,
            screens = ResolvedScreenTheme.FALLBACK,
            sourcePack = "builtin",
            sourceResource = "builtin:empty",
            elementCount = 1,
        )

        val EMPTY = ThemeSnapshot(
            revision = 0,
            themes = emptyMap(),
            activeTheme = FALLBACK_THEME,
        )
    }
}
