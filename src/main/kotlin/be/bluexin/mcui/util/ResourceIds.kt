package be.bluexin.mcui.util

import be.bluexin.mcui.Constants
import net.minecraft.resources.ResourceLocation

fun mcuiId(path: String): ResourceLocation =
    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path)

fun legacyMcuiId(path: String): ResourceLocation =
    ResourceLocation.fromNamespaceAndPath(Constants.LEGACY_MOD_ID, path)

fun ResourceLocation.appendPath(suffix: String): ResourceLocation =
    ResourceLocation.fromNamespaceAndPath(namespace, path + suffix)
