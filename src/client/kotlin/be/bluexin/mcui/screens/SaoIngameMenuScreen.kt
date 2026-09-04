/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.LanguageSelectScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen
import net.minecraft.network.chat.Component

/** Five-category SAO pause menu with final-position hitboxes and lightweight panel transitions. */
class SaoIngameMenuScreen : Screen(Component.translatable("menu.game")), SaoScreenSurface {
    private var selected = Category.PROFILE
    private val animation = SaoScreenAnimation()
    private val categoryButtons = linkedMapOf<Category, SaoIconButton>()
    private val actionButtons = mutableListOf<SaoIconButton>()
    private lateinit var layout: Layout

    override fun init() {
        animation.reset()
        categoryButtons.clear()
        actionButtons.clear()
        layout = createLayout()
        Category.entries.forEachIndexed { index, category ->
            val button = SaoIconButton(
                x = layout.categoryX,
                y = layout.contentY + index * layout.categoryStep,
                width = layout.categorySize,
                height = layout.categorySize,
                message = Component.translatable(category.translation),
                icon = category.icon,
                compact = true,
                selected = category == selected,
            ) {
                selectCategory(category)
            }
            button.tooltip = Tooltip.create(Component.translatable(category.translation))
            categoryButtons[category] = addRenderableWidget(button)
        }
        rebuildActions()
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
        val style = SaoUiStyle.current()
        val panelProgress = animation.panelProgress(style.animation.categoryMillis)
        val slide = ((1f - panelProgress) * style.animation.panelSlidePixels).toInt()
        SaoUiStyle.renderPanel(
            graphics,
            layout.panelX + slide,
            layout.panelY,
            layout.panelWidth,
            layout.panelHeight,
            dark = true,
            alpha = panelProgress,
        )
        val title = SaoUiStyle.fitText(Component.translatable(selected.translation), layout.actionWidth)
        graphics.drawString(font, title, layout.actionX, layout.panelY + 8, SaoUiStyle.LIGHT_TEXT, true)

        categoryButtons.values.forEachIndexed { index, button ->
            button.setAlpha(animation.screenProgress(style.animation.screenFadeMillis, index * style.animation.buttonStaggerMillis))
        }
        actionButtons.forEachIndexed { index, button ->
            button.setAlpha(animation.panelProgress(style.animation.categoryMillis + index * style.animation.buttonStaggerMillis))
        }
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun selectCategory(category: Category) {
        if (category == selected) return
        selected = category
        categoryButtons.forEach { (value, button) -> button.selected = value == category }
        animation.restartPanel()
        rebuildActions()
    }

    private fun rebuildActions() {
        actionButtons.forEach(::removeWidget)
        actionButtons.clear()
        actions(selected).forEachIndexed { index, action ->
            val button = SaoIconButton(
                x = layout.actionX,
                y = layout.contentY + index * layout.actionStep,
                width = layout.actionWidth,
                height = layout.actionHeight,
                message = action.label,
                icon = action.icon,
                action = action.action,
            )
            button.active = action.enabled
            if (!action.enabled) button.tooltip = Tooltip.create(Component.translatable("mcui.screen.unavailable"))
            actionButtons += addRenderableWidget(button)
        }
        actionButtons += addRenderableWidget(
            SaoIconButton(
                x = layout.actionX,
                y = layout.contentY + 6 * layout.actionStep,
                width = layout.actionWidth,
                height = layout.actionHeight,
                message = Component.translatable("menu.returnToGame"),
                icon = SaoIcon.CONFIRM,
            ) { onClose() },
        )
    }

    private fun createLayout(): Layout {
        val style = SaoUiStyle.current()
        val margin = style.spacing.margin.coerceAtMost((width / 5).coerceAtLeast(1))
        val panelWidth = minOf(286, (width - margin * 2).coerceAtLeast(90))
        val panelHeight = minOf(190, (height - margin * 2).coerceAtLeast(120))
        val panelX = (width - panelWidth) / 2
        val panelY = (height - panelHeight) / 2
        val padding = style.spacing.padding
        val categorySize = 24
        val categoryStep = ((panelHeight - padding * 2) / Category.entries.size).coerceIn(24, 29)
        val contentY = panelY + padding + 20
        val actionX = panelX + padding + categorySize + style.spacing.buttonGap + 3
        return Layout(
            panelX = panelX,
            panelY = panelY,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
            categoryX = panelX + padding,
            categorySize = categorySize,
            categoryStep = categoryStep,
            contentY = contentY,
            actionX = actionX,
            actionWidth = (panelX + panelWidth - padding - actionX).coerceAtLeast(42),
            actionHeight = 20,
            actionStep = 23,
        )
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
                            client.reportingContext.draftReportHandled(
                                client,
                                this,
                                Runnable { disconnectFromWorld(client) },
                                true,
                            )
                        },
                    )
                },
            )
        }
    }

    private fun disconnectFromWorld(client: Minecraft) {
        val localServer = client.isLocalServer
        val server = client.currentServer
        client.level?.disconnect()
        if (localServer) client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel")))
        else client.disconnect()

        val title = SaoTitleScreen()
        client.setScreen(
            when {
                localServer || server?.isRealm == true -> title
                else -> JoinMultiplayerScreen(title)
            },
        )
    }

    private data class MenuAction(
        val label: Component,
        val icon: SaoIcon,
        val enabled: Boolean = true,
        val action: () -> Unit,
    )

    private data class Layout(
        val panelX: Int,
        val panelY: Int,
        val panelWidth: Int,
        val panelHeight: Int,
        val categoryX: Int,
        val categorySize: Int,
        val categoryStep: Int,
        val contentY: Int,
        val actionX: Int,
        val actionWidth: Int,
        val actionHeight: Int,
        val actionStep: Int,
    )

    private enum class Category(val icon: SaoIcon, val translation: String) {
        PROFILE(SaoIcon.PROFILE, "sao.element.profile"),
        SOCIAL(SaoIcon.SOCIAL, "sao.element.social"),
        MESSAGE(SaoIcon.MESSAGE, "sao.element.message"),
        NAVIGATION(SaoIcon.NAVIGATION, "sao.element.navigation"),
        SETTINGS(SaoIcon.SETTINGS, "sao.element.settings"),
    }
}
