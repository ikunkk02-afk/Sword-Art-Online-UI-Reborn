/* SPDX-License-Identifier: GPL-3.0-or-later */
package be.bluexin.mcui.screens

import be.bluexin.mcui.Constants
import be.bluexin.mcui.config.ConfigPaths
import be.bluexin.mcui.config.SaoFriendStore
import net.minecraft.network.chat.Component

object SaoFriends {
    val store by lazy {
        SaoFriendStore(
            ConfigPaths.root.resolve("friends.json"),
            net.fabricmc.loader.api.FabricLoader.getInstance().configDir.resolve("saoui").resolve("friend_list.cfg"),
        ).also {
            runCatching { it.load() }.onFailure { error -> Constants.LOG.error("Unable to read friend list", error) }
        }
    }

    fun change(action: SaoFriendStore.() -> Unit) {
        runCatching { store.action() }.onFailure {
            Constants.LOG.error("Unable to save friend list", it)
            SaoNotificationAlert.show(SaoIcon.FRIEND, Component.translatable("sao.element.friends"),
                Component.translatable("mcui.friends.save_failed"))
        }
    }
}
