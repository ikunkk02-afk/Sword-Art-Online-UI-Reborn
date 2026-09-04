/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric.client

import be.bluexin.mcui.Constants
import be.bluexin.mcui.fabric.client.hud.MCUIHudRenderer
import be.bluexin.mcui.fabric.client.resources.ClientResourceReloads
import be.bluexin.mcui.screens.SaoSounds
import net.fabricmc.api.ClientModInitializer

object MCUIFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        SaoSounds.register()
        ClientResourceReloads.register()
        MCUIHudRenderer.register()
        Constants.LOG.info("MCUI client foundation initialized")
    }
}
