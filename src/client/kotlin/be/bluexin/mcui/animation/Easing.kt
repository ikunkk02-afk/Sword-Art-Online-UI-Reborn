/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.animation

import be.bluexin.mcui.themes.AnimationEasing

/** Runtime easing kept independent from the historical reflection animator. */
fun interface Easing {
    fun transform(progress: Float): Float

    companion object {
        fun resolve(value: AnimationEasing): Easing = Easing { raw ->
            val progress = raw.coerceIn(0f, 1f)
            when (value) {
                AnimationEasing.UNSUPPORTED -> progress
                AnimationEasing.LINEAR -> progress
                AnimationEasing.EASE_IN, AnimationEasing.QUAD_IN -> progress * progress
                AnimationEasing.EASE_OUT, AnimationEasing.QUAD_OUT -> 1f - (1f - progress) * (1f - progress)
                AnimationEasing.EASE_IN_OUT, AnimationEasing.QUAD_IN_OUT -> if (progress < 0.5f) {
                    2f * progress * progress
                } else {
                    val inverse = -2f * progress + 2f
                    1f - inverse * inverse / 2f
                }
                AnimationEasing.CUBIC_IN -> progress * progress * progress
                AnimationEasing.CUBIC_OUT -> {
                    val inverse = 1f - progress
                    1f - inverse * inverse * inverse
                }
                AnimationEasing.CUBIC_IN_OUT -> if (progress < 0.5f) {
                    4f * progress * progress * progress
                } else {
                    val inverse = -2f * progress + 2f
                    1f - inverse * inverse * inverse / 2f
                }
            }
        }
    }
}
