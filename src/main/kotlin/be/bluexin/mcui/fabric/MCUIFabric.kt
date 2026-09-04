/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.fabric

import be.bluexin.mcui.MCUICore
import net.fabricmc.api.ModInitializer

object MCUIFabric : ModInitializer {
    override fun onInitialize() = MCUICore.init()
}
