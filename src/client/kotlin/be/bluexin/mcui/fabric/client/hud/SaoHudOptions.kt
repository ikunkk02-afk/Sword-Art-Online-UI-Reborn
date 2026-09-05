package be.bluexin.mcui.fabric.client.hud

import be.bluexin.mcui.config.SaoOption
import be.bluexin.mcui.themes.HudPartType

/** A disabled render switch hides the replacement; it does not bring vanilla back. */
object SaoHudOptions {
    fun enabled(part: HudPartType): Boolean {
        if (SaoOption.VANILLA_UI()) return false
        return when (part) {
            HudPartType.HEALTH_BOX -> SaoOption.RENDER_HEALTH()
            HudPartType.ARMOR -> SaoOption.RENDER_ARMOR()
            HudPartType.HOTBAR -> SaoOption.RENDER_HOTBAR() && !SaoOption.DEFAULT_HOTBAR()
            HudPartType.AIR -> SaoOption.RENDER_AIR()
            HudPartType.FOOD -> SaoOption.RENDER_FOOD()
            HudPartType.EXPERIENCE -> SaoOption.RENDER_EXPERIENCE()
            HudPartType.CROSS_HAIR -> SaoOption.RENDER_CROSSHAIRS()
            HudPartType.EFFECTS -> SaoOption.RENDER_POTION_ICONS()
            HudPartType.JUMP_BAR -> SaoOption.RENDER_JUMPBAR()
            HudPartType.MOUNT_HEALTH -> SaoOption.RENDER_HEALTHMOUNT()
            HudPartType.ENTITY_HEALTH_HUD -> SaoOption.ENEMY_ONSCREEN_HEALTH()
            else -> true
        }
    }
}
