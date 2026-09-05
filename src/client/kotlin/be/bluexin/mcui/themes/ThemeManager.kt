/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import java.util.concurrent.atomic.AtomicReference

/** Owns one immutable snapshot and publishes it with one atomic reference swap. */
class ThemeManager(
    private var preferredTheme: ThemeId?,
) {
    private val snapshotReference = AtomicReference(ThemeSnapshot.EMPTY)

    val snapshot: ThemeSnapshot get() = snapshotReference.get()
    val activeTheme: ResolvedTheme get() = snapshot.activeTheme

    fun select(id: ThemeId): Boolean {
        val current = snapshot
        val theme = current.themes[id] ?: return false
        preferredTheme = id
        snapshotReference.set(current.copy(activeTheme = theme))
        return true
    }

    /** Returns false only for a loader-wide fatal failure, preserving the previous good snapshot. */
    fun apply(revision: Long, result: ThemeLoadResult): Boolean {
        if (result.fatalError != null) return false
        val immutableThemes = result.themes.toMap()
        val active = preferredTheme?.let(immutableThemes::get) ?: ThemeSnapshot.FALLBACK_THEME
        snapshotReference.set(ThemeSnapshot(revision, immutableThemes, active))
        return true
    }
}
