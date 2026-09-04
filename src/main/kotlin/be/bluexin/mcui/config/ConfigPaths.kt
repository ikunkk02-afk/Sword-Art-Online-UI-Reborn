/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.config

import be.bluexin.mcui.Constants
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object ConfigPaths {
    val root: Path
        get() = FabricLoader.getInstance().configDir.resolve(Constants.MOD_ID)

    fun prepare(): Path = root.also { Files.createDirectories(it) }
}
