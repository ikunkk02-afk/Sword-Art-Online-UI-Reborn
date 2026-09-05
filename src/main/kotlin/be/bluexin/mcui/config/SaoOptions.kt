/* Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé, Tencao
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package be.bluexin.mcui.config

import be.bluexin.mcui.Constants
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException

enum class SaoOptionCategory(val translation: String, val parent: SaoOptionCategory? = null) {
    UI("optCatUI"), THEME("optTheme"), ENTITIES("optEntities"), HEALTH("optCatHealth"),
    HOTBAR("optCatHotBar"), EFFECTS("optCatEffects"), MISC("optCatMisc"), DEBUG("optCatDebug"),
    ENTITY_HEALTH("optionEntityHealthBars", ENTITIES), CRYSTALS("optionEntityCrystals", ENTITIES),
    RENDER("optionRenderSystem", DEBUG);
}

/** The 1.12.2 option names/defaults are retained, with later renderer switches kept under DEBUG. */
enum class SaoOption(val translation: String, val defaultValue: Boolean, val category: SaoOptionCategory) {
    UI_ONLY("optionUIOnly", false, SaoOptionCategory.UI),
    DEFAULT_INVENTORY("optionDefaultInv", true, SaoOptionCategory.UI),
    DEFAULT_DEATH_SCREEN("optionDefaultDeath", false, SaoOptionCategory.UI),
    DEFAULT_DEBUG("optionDefaultDebug", false, SaoOptionCategory.UI),
    FORCE_HUD("optionForceHud", true, SaoOptionCategory.UI),
    LOGOUT("optionLogout", true, SaoOptionCategory.UI),
    GUI_PAUSE("optionGuiPause", false, SaoOptionCategory.UI),
    UI_MOVEMENT("optionUIMovement", true, SaoOptionCategory.UI),
    VANILLA_UI("optionDefaultUI", false, SaoOptionCategory.THEME),
    SMOOTH_HEALTH("optionSmoothHealth", true, SaoOptionCategory.HEALTH),
    REMOVE_HPXP("optionLightHud", false, SaoOptionCategory.HEALTH),
    ALT_ABSORB_POS("optionAltAbsorbPos", false, SaoOptionCategory.HEALTH),
    ENEMY_ONSCREEN_HEALTH("optionEnemyOnscreenHealth", true, SaoOptionCategory.HEALTH),
    HIDE_OFFLINE_PARTY("optionHideOfflineParty", false, SaoOptionCategory.HEALTH),
    INNOCENT_HEALTH("optionInnocentHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    VIOLENT_HEALTH("optionViolentHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    KILLER_HEALTH("optionKillerHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    BOSS_HEALTH("optionBossHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    CREATIVE_HEALTH("optionCreativeHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    OP_HEALTH("optionOPHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    INVALID_HEALTH("optionInvalidHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    DEV_HEALTH("optionDevHealthBars", true, SaoOptionCategory.ENTITY_HEALTH),
    INNOCENT_CRYSTAL("optionInnocentCrystal", true, SaoOptionCategory.CRYSTALS),
    VIOLENT_CRYSTAL("optionViolentCrystal", true, SaoOptionCategory.CRYSTALS),
    KILLER_CRYSTAL("optionKillerCrystal", true, SaoOptionCategory.CRYSTALS),
    BOSS_CRYSTAL("optionBossCrystal", true, SaoOptionCategory.CRYSTALS),
    CREATIVE_CRYSTAL("optionCreativeCrystal", true, SaoOptionCategory.CRYSTALS),
    OP_CRYSTAL("optionOPCrystal", true, SaoOptionCategory.CRYSTALS),
    INVALID_CRYSTAL("optionInvalidCrystal", true, SaoOptionCategory.CRYSTALS),
    DEV_CRYSTAL("optionDevCrystal", true, SaoOptionCategory.CRYSTALS),
    DEFAULT_HOTBAR("optionDefaultHotbar", false, SaoOptionCategory.HOTBAR),
    HOR_HOTBAR("optionHorHotbar", false, SaoOptionCategory.HOTBAR),
    VER_HOTBAR("optionVerHotbar", true, SaoOptionCategory.HOTBAR),
    SPINNING_CRYSTALS("optionSpinning", true, SaoOptionCategory.EFFECTS),
    PARTICLES("optionParticles", true, SaoOptionCategory.EFFECTS),
    SOUND_EFFECTS("optionSounds", true, SaoOptionCategory.EFFECTS),
    MOUSE_OVER_EFFECT("optionMouseOver", true, SaoOptionCategory.EFFECTS),
    AGGRO_SYSTEM("optionAggro", true, SaoOptionCategory.MISC),
    MOUNT_STAT_VIEW("optionMountStatView", true, SaoOptionCategory.MISC),
    CUSTOM_FONT("optionCustomFont", false, SaoOptionCategory.MISC),
    TEXT_SHADOW("optionTextShadow", true, SaoOptionCategory.MISC),
    RENDER_CROSSHAIRS("optionRenderCrosshairs", true, SaoOptionCategory.RENDER),
    RENDER_ARMOR("optionRenderArmor", true, SaoOptionCategory.RENDER),
    RENDER_HOTBAR("optionRenderHotbar", true, SaoOptionCategory.RENDER),
    RENDER_AIR("optionRenderAir", true, SaoOptionCategory.RENDER),
    RENDER_POTION_ICONS("optionRenderPotionIcons", true, SaoOptionCategory.RENDER),
    RENDER_HEALTH("optionRenderHealth", true, SaoOptionCategory.RENDER),
    RENDER_FOOD("optionRenderFood", true, SaoOptionCategory.RENDER),
    RENDER_EXPERIENCE("optionRenderExp", true, SaoOptionCategory.RENDER),
    RENDER_JUMPBAR("optionRenderJumpbar", true, SaoOptionCategory.RENDER),
    RENDER_HEALTHMOUNT("optionRenderHealthMount", true, SaoOptionCategory.RENDER);

    operator fun invoke(): Boolean = SaoOptions.store[this]
}

@Serializable
data class SaoSettings(val theme: String = "mcui:saoui_reborn", val options: Map<String, Boolean> = emptyMap())

/** File location is injected so validation never touches the player's configuration. */
class SaoOptionStore(private val file: Path? = null) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    var settings = SaoSettings()
        private set
    var revision = 0L
        private set

    operator fun get(option: SaoOption) = settings.options[option.name] ?: option.defaultValue

    fun load() {
        if (file == null || !Files.exists(file)) return
        val loaded = json.decodeFromString<SaoSettings>(Files.readString(file))
        settings = normalize(loaded)
        revision++
    }

    fun set(option: SaoOption, enabled: Boolean) {
        val values = settings.options.toMutableMap()
        if (option.category == SaoOptionCategory.HOTBAR) {
            // The original restricted category always selects the clicked option.
            SaoOption.entries.filter { it.category == SaoOptionCategory.HOTBAR }.forEach { values[it.name] = it == option }
        } else values[option.name] = enabled
        publish(settings.copy(options = values))
    }

    fun selectTheme(theme: String) {
        publish(settings.copy(theme = theme, options = settings.options + (SaoOption.VANILLA_UI.name to false)))
    }

    private fun normalize(value: SaoSettings): SaoSettings {
        val migratedOptions = value.options.toMutableMap()
        // Early 1.21.1 builds called the original FORCE_HUD option ALWAYS_SHOW.
        // Preserve that user's selection while returning to the 1.12.2 persisted name.
        if ("FORCE_HUD" !in migratedOptions && "ALWAYS_SHOW" in migratedOptions) {
            migratedOptions[SaoOption.FORCE_HUD.name] = migratedOptions.getValue("ALWAYS_SHOW")
        }
        migratedOptions.remove("ALWAYS_SHOW")
        val hotbar = SaoOption.entries.filter { it.category == SaoOptionCategory.HOTBAR }
        val selected = hotbar.firstOrNull { migratedOptions[it.name] == true } ?: SaoOption.VER_HOTBAR
        return value.copy(options = migratedOptions + hotbar.associate { it.name to (it == selected) })
    }

    private fun publish(value: SaoSettings) {
        if (file != null) {
            Files.createDirectories(file.toAbsolutePath().parent)
            val temporary = Files.createTempFile(file.toAbsolutePath().parent, "sao-options-", ".tmp")
            try {
                Files.writeString(temporary, json.encodeToString(value))
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally { Files.deleteIfExists(temporary) }
        }
        settings = value
        revision++
    }
}

object SaoOptions {
    var store = SaoOptionStore()
        private set

    fun initialize() {
        store = SaoOptionStore(ConfigPaths.root.resolve("options.json"))
        runCatching { store.load() }.onFailure {
            Constants.LOG.error("Could not read SAO options; using defaults, existing file retained", it)
        }
    }
}
