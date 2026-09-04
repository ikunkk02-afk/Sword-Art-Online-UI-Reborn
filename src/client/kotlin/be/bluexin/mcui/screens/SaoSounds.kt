/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import be.bluexin.mcui.util.legacyMcuiId
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.sounds.SoundEvent

/** Original SAOUI sound events, kept separate so each legacy interaction uses its own cue. */
enum class SaoSound(val path: String) {
    CONFIRM("confirm"),
    DIALOG_CLOSE("dialog_close"),
    MENU_POPUP("menu_popup"),
    MESSAGE("message"),
    ORB_DROPDOWN("orb_dropdown"),
    PARTICLES_DEATH("particles_death");

    internal val event: SoundEvent = SoundEvent.createVariableRangeEvent(legacyMcuiId(path))
}

object SaoSounds {
    private var registered = false

    @JvmStatic
    fun register() {
        if (registered) return
        SaoSound.entries.forEach { sound ->
            Registry.register(BuiltInRegistries.SOUND_EVENT, legacyMcuiId(sound.path), sound.event)
        }
        registered = true
    }

    @JvmStatic
    fun play(sound: SaoSound) {
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(sound.event, 1f))
    }
}
