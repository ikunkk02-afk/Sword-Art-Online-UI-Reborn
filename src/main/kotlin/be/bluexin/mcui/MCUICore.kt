/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui

import be.bluexin.mcui.config.ConfigPaths
import net.fabricmc.loader.api.FabricLoader
import java.util.concurrent.atomic.AtomicBoolean

object MCUICore {
    private val initialized = AtomicBoolean(false)

    fun init() {
        if (!initialized.compareAndSet(false, true)) {
            Constants.LOG.debug("MCUI core initialization was requested more than once")
            return
        }

        val loader = FabricLoader.getInstance()
        val version = loader.getModContainer(Constants.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("unknown")

        ConfigPaths.prepare()
        Constants.LOG.info(
            "{} {} initialized on Fabric ({})",
            Constants.MOD_NAME,
            version,
            if (loader.isDevelopmentEnvironment) "development" else "production",
        )
    }
}
