/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.LanguageSelectScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen
import net.minecraft.network.chat.Component

/** Stable-branch five-category in-game menu, with immediate/static expansion. */
class SaoIngameMenuScreen : Screen(Component.translatable("menu.game")) {
    private var selected = Category.PROFILE

    override fun init() {
        val rootX = width / 2 - 122
        val rootY = (height - Category.entries.size * 27) / 2
        Category.entries.forEachIndexed { index, category ->
            val button = SaoIconButton(
                x = rootX,
                y = rootY + index * 27,
                width = 24,
                height = 24,
                message = Component.translatable(category.translation),
                icon = category.icon,
                compact = true,
            ) {
                selected = category
                rebuildWidgets()
            }
            addRenderableWidget(button)
        }

        val actionX = rootX + 31
        val actionY = rootY
        actions(selected).forEachIndexed { index, action ->
            val button = SaoIconButton(
                x = actionX,
                y = actionY + index * 24,
                width = 190,
                height = 21,
                message = action.label,
                icon = action.icon,
                action = action.action,
            )
            button.active = action.enabled
            addRenderableWidget(button)
        }

        addRenderableWidget(
            SaoIconButton(
                x = actionX,
                y = actionY + 6 * 24,
                width = 190,
                height = 21,
                message = Component.translatable("menu.returnToGame"),
                icon = SaoIcon.CONFIRM,
            ) { onClose() },
        )
    }

    override fun onClose() {
        minecraft?.setScreen(null)
    }

    override fun isPauseScreen(): Boolean = true

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        SaoUiStyle.renderInWorldBackground(graphics, width, height)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        val rootX = width / 2 - 122
        val rootY = (height - Category.entries.size * 27) / 2
        SaoUiStyle.renderPanel(graphics, rootX - 7, rootY - 24, 234, 190)
        graphics.drawString(font, Component.translatable(selected.translation), rootX + 31, rootY - 16, SaoUiStyle.TEXT, true)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun actions(category: Category): List<MenuAction> {
        val client = requireNotNull(minecraft)
        val player = client.player
        val connection = client.connection
        return when (category) {
            Category.PROFILE -> listOf(
                MenuAction(Component.translatable("container.inventory"), SaoIcon.ITEMS, player != null) {
                    client.setScreen(SaoInventoryScreen())
                },
                MenuAction(Component.translatable("gui.advancements"), SaoIcon.SKILLS, connection != null) {
                    client.setScreen(AdvancementsScreen(connection!!.advancements, this))
                },
                MenuAction(Component.translatable("gui.stats"), SaoIcon.PROFILE, player != null) {
                    client.setScreen(StatsScreen(this, player!!.stats))
                },
                MenuAction(Component.translatable("sao.element.equipment"), SaoIcon.EQUIPMENT, player != null) {
                    client.setScreen(SaoInventoryScreen())
                },
                MenuAction(Component.translatable("sao.element.accessory"), SaoIcon.ACCESSORY, false) {},
            )
            Category.SOCIAL -> listOf(
                MenuAction(Component.translatable("gui.socialInteractions.title"), SaoIcon.SOCIAL, connection != null) {
                    client.setScreen(SocialInteractionsScreen(this))
                },
                MenuAction(Component.translatable("sao.element.guild"), SaoIcon.GUILD, false) {},
                MenuAction(Component.translatable("sao.element.party"), SaoIcon.PARTY, false) {},
                MenuAction(Component.translatable("sao.element.friends"), SaoIcon.FRIEND, false) {},
                MenuAction(Component.translatable("sao.element.invite"), SaoIcon.INVITE, false) {},
            )
            Category.MESSAGE -> listOf(
                MenuAction(Component.translatable("chat_screen.title"), SaoIcon.MESSAGE, connection != null) {
                    client.setScreen(ChatScreen(""))
                },
                MenuAction(Component.translatable("sao.element.message_box"), SaoIcon.MESSAGE_RECEIVED, false) {},
            )
            Category.NAVIGATION -> listOf(
                MenuAction(Component.translatable("sao.element.quest"), SaoIcon.QUEST, connection != null) {
                    client.setScreen(AdvancementsScreen(connection!!.advancements, this))
                },
                MenuAction(Component.translatable("sao.element.field_map"), SaoIcon.FIELD_MAP, false) {},
                MenuAction(Component.translatable("sao.element.dungeon_map"), SaoIcon.DUNGEON_MAP, false) {},
            )
            Category.SETTINGS -> listOf(
                MenuAction(Component.translatable("menu.options"), SaoIcon.OPTION) {
                    client.setScreen(OptionsScreen(this, client.options))
                },
                MenuAction(Component.translatable("options.language"), SaoIcon.MESSAGE) {
                    client.setScreen(LanguageSelectScreen(this, client.options, client.languageManager))
                },
                MenuAction(Component.translatable("options.accessibility.title"), SaoIcon.HELP) {
                    client.setScreen(AccessibilityOptionsScreen(this, client.options))
                },
                MenuAction(Component.translatable("sao.element.menu"), SaoIcon.HELP) {
                    client.setScreen(SaoNoticeScreen(this))
                },
                MenuAction(Component.translatable("menu.disconnect"), SaoIcon.LOGOUT, player != null) {
                    client.setScreen(
                        SaoConfirmationScreen(
                            parent = this,
                            title = Component.translatable("mcui.screen.logout.title"),
                            body = Component.translatable("mcui.screen.logout.body"),
                        ) {
                            client.disconnect(SaoTitleScreen())
                        },
                    )
                },
            )
        }
    }

    private data class MenuAction(
        val label: Component,
        val icon: SaoIcon,
        val enabled: Boolean = true,
        val action: () -> Unit,
    )

    private enum class Category(val icon: SaoIcon, val translation: String) {
        PROFILE(SaoIcon.PROFILE, "sao.element.profile"),
        SOCIAL(SaoIcon.SOCIAL, "sao.element.social"),
        MESSAGE(SaoIcon.MESSAGE, "sao.element.message"),
        NAVIGATION(SaoIcon.NAVIGATION, "sao.element.navigation"),
        SETTINGS(SaoIcon.SETTINGS, "sao.element.settings"),
    }
}
