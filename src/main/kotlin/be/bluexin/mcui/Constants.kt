/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Constants {
    const val MOD_ID = "mcui"
    const val LEGACY_MOD_ID = "saoui"
    const val MOD_NAME = "Sword Art Online UI: Reborn"

    @JvmField
    val LOG: Logger = LoggerFactory.getLogger(MOD_ID)
}

fun Any.logger(): Logger = LoggerFactory.getLogger("${Constants.MOD_ID}/${this::class.simpleName?.removeSuffix("Impl")}")
