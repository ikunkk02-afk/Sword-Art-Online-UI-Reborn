/* SPDX-License-Identifier: GPL-3.0-or-later */
package be.bluexin.mcui.screens

import be.bluexin.mcui.config.SaoOption
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.player.Player
import java.util.Locale

/** DefaultStatsProvider's six player rows and mount-specific rows. */
object SaoProfileStats {
    fun lines(player: Player): List<Component> {
        val mount = (player.rootVehicle as? LivingEntity)?.takeIf { it !== player && SaoOption.MOUNT_STAT_VIEW() }
        fun row(key: String, value: String): Component = Component.translatable(key).append(": $value")
        fun number(value: Double) = ((value * 1000).toInt() / 1000f).toString()
        return if (mount != null) buildList {
            add(Component.translatable("displayName").append(": ").append(mount.name))
            add(row("displayHpLong", String.format(Locale.ROOT, "%.1f/%s", mount.health + 0.05, number(mount.maxHealth.toDouble()))))
            add(row("displayResLong", number(mount.armorValue.toDouble())))
            add(row("displaySpdLong", String.format(Locale.ROOT, "%.3f", mount.getAttributeValue(Attributes.MOVEMENT_SPEED))))
            if (mount is AbstractHorse) add(row("displayJmpLong", String.format(Locale.ROOT, "%.3f", mount.getAttributeValue(Attributes.JUMP_STRENGTH))))
        } else listOf(
            row("displayLvLong", player.experienceLevel.toString()),
            row("displayXpLong", (player.experienceProgress * 100).toInt().toString()),
            row("displayHpLong", "${number(player.health.toDouble())}/${number(player.maxHealth.toDouble())}"),
            row("displayStrLong", number(player.getAttributeValue(Attributes.ATTACK_DAMAGE))),
            row("displayDexLong", number(player.speed * 10.0)),
            row("displayResLong", number(player.armorValue.toDouble())),
        )
    }
}
