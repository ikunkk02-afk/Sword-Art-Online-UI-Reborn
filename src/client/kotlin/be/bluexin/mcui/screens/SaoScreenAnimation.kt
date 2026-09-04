/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import kotlin.math.pow

/** Small monotonic screen transition. It never owns or changes widget hitboxes. */
class SaoScreenAnimation {
    private var openedAt = System.nanoTime()
    private var panelChangedAt = openedAt

    fun reset() {
        openedAt = System.nanoTime()
        panelChangedAt = openedAt
    }

    fun restartPanel() {
        panelChangedAt = System.nanoTime()
    }

    fun screenProgress(durationMillis: Int, delayMillis: Int = 0): Float =
        progress(openedAt, durationMillis, delayMillis)

    fun panelProgress(durationMillis: Int): Float = progress(panelChangedAt, durationMillis, 0)

    private fun progress(startedAt: Long, durationMillis: Int, delayMillis: Int): Float {
        if (durationMillis <= 0) return 1f
        val elapsedMillis = (System.nanoTime() - startedAt).coerceAtLeast(0L) / 1_000_000.0
        val linear = ((elapsedMillis - delayMillis) / durationMillis).toFloat().coerceIn(0f, 1f)
        return 1f - (1f - linear).pow(3)
    }
}
