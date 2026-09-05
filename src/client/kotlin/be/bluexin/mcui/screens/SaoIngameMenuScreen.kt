/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

import be.bluexin.mcui.config.SaoOption
import be.bluexin.mcui.config.SaoOptionCategory
import be.bluexin.mcui.config.SaoOptions
import be.bluexin.mcui.themes.MCUIThemes
import com.tencao.saomclib.Client as SaoMcClient
import com.tencao.saomclib.capabilities.getPartyCapability
import com.tencao.saomclib.packets.PartyType
import com.tencao.saomclib.packets.Type
import com.tencao.saomclib.packets.to_server.updateServer
import com.tencao.saomclib.party.PlayerInfo
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A source-derived port of the 1.16.5 CoreGUI/CategoryButton element tree.
 * Coordinates remain relative to one moving root; there is intentionally no
 * modern fixed panel or flattened action list.
 */
class SaoIngameMenuScreen : Screen(Component.translatable("menu.game")), SaoScreenSurface {
    private val topLevel = mutableListOf<LegacyMenuNode>()
    private var baseRootX = 0.0
    private var baseRootY = 0.0
    private var movementX = 0.0
    private var movementY = 0.0
    private var movementFromX = 0.0
    private var movementFromY = 0.0
    private var movementToX = 0.0
    private var movementToY = 0.0
    private var movementStartedAt = 0L
    private var movementAnimating = false
    private var parallaxX = 0.0
    private var parallaxY = 0.0
    private var previousMouseX: Double? = null
    private var previousMouseY: Double? = null
    private var capturedPlayer: net.minecraft.world.entity.player.Player? = null
    private var capturedYaw = 0f
    private var capturedPitch = 0f
    private var closingForChild = false
    private var resumeAfterPopup = false
    private var keyboardSelected: LegacyMenuNode? = null
    private var partySignature = ""
    private var menuOpenedAt = 0L

    override fun init() {
        if (resumeAfterPopup && topLevel.isNotEmpty()) {
            resumeAfterPopup = false
            capturePlayerView()
            previousMouseX = null
            previousMouseY = null
            return
        }
        closeAll(playSound = false)
        topLevel.clear()
        buildOriginalTree()
        partySignature = currentPartySignature()
        normalizeLabelWidths(topLevel)

        // IngameMenu.init, origin/1.16.5.
        baseRootX = width / 2.0 - LegacySaoMetrics.ROOT_CENTER_X_OFFSET
        baseRootY = (height - topLevel.size * LegacySaoMetrics.ICON_BOUND) / 2.0
        movementX = baseRootX
        movementY = baseRootY
        movementFromX = baseRootX
        movementFromY = baseRootY
        movementToX = baseRootX
        movementToY = baseRootY
        movementAnimating = false
        parallaxX = 0.0
        parallaxY = 0.0
        previousMouseX = null
        previousMouseY = null
        keyboardSelected = null
        menuOpenedAt = System.nanoTime()
        capturePlayerView()
        SaoSounds.play(SaoSound.ORB_DROPDOWN)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // CoreGUI rendered directly over the world; it had no menu backdrop panel.
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        refreshPartyMenuIfNeeded()
        updatePlayerView(mouseX, mouseY)
        updateMovement()
        renderTree(graphics, mouseX.toDouble(), mouseY.toDouble(), includeProfile = true)
    }

    /** Used by a legacy popup so its parent remains visible without recapturing player view. */
    internal fun renderBehindPopup(graphics: GuiGraphics) {
        updateMovement()
        renderTree(graphics, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, includeProfile = true)
    }

    internal fun preparePopupReturn() {
        resumeAfterPopup = true
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button)
        val hit = collectEntries(System.nanoTime()).asReversed()
            .firstOrNull { it.node.listed && it.contains(mouseX, mouseY) }
        val selectedTop = topLevel.firstOrNull { it.open }
        if (hit == null) {
            selectedTop?.let { closeNode(it, playSound = false) }
            return false
        }
        if (selectedTop != null && hit.node !== selectedTop && !hit.node.isDescendantOf(selectedTop)) {
            closeNode(selectedTop, playSound = false)
        }
        if (!hit.node.enabled) return true

        val node = hit.node
        keyboardSelected = node
        if (node.children.isNotEmpty() || node.loadChildren != null) {
            if (node.open) closeNode(node, playSound = true) else openNode(node, playSound = true)
            return true
        }
        node.action?.invoke()
        return true
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        val parent = deepestOpenParent() ?: return false
        val count = parent.children.count { it.listed }
        if (count < LegacySaoMetrics.MAX_VISIBLE_CHILDREN) return false
        val revealDuration = (LegacySaoMetrics.MAX_VISIBLE_CHILDREN - 1L) * LegacySaoMetrics.CHILD_REVEAL_MILLIS
        val revealElapsed = (System.nanoTime() - parent.openedAt).coerceAtLeast(0L) / 1_000_000L
        // CategoryButton consumed wheel input while IndexedScheduledCounter was active.
        if (revealElapsed < revealDuration) return true
        if (verticalAmount > 0.0) parent.scroll-- else if (verticalAmount < 0.0) parent.scroll++ else return false
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val oldX = previousMouseX
        val oldY = previousMouseY
        if (SaoOption.UI_MOVEMENT() && oldX != null && oldY != null) {
            parallaxX += (mouseX - oldX) * LegacySaoMetrics.ROOT_MOUSE_MOVEMENT
            parallaxY += (mouseY - oldY) * LegacySaoMetrics.ROOT_MOUSE_MOVEMENT
        }
        previousMouseX = mouseX
        previousMouseY = mouseY
        super.mouseMoved(mouseX, mouseY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) return super.keyPressed(keyCode, scanCode, modifiers)
        val deepest = deepestOpenParent()
        val candidates = if (deepest == null) topLevel else orderedVisibleChildren(deepest)
        if (keyCode == 265 || keyCode == 87 || keyCode == 264 || keyCode == 83) {
            if (candidates.isEmpty()) return true
            val current = candidates.indexOf(keyboardSelected)
            val next = if (keyCode == 265 || keyCode == 87) {
                if (current <= 0) candidates.lastIndex else current - 1
            } else {
                if (current < 0 || current >= candidates.lastIndex) 0 else current + 1
            }
            keyboardSelected = candidates[next]
            return true
        }
        if (keyCode == 263 || keyCode == 65) {
            deepest?.let { closeNode(it, playSound = false) }
            keyboardSelected = deepest?.parent ?: keyboardSelected
            return deepest != null
        }
        if (keyCode == 262 || keyCode == 68 || keyCode == 257 || keyCode == 335 || keyCode == 32) {
            val selected = keyboardSelected ?: return false
            if (!selected.enabled) return true
            if (selected.children.isNotEmpty() || selected.loadChildren != null) {
                if (!selected.open) openNode(selected, playSound = false)
            } else selected.action?.invoke()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun onClose() {
        closingForChild = false
        restorePlayerView()
        minecraft?.setScreen(null)
    }

    override fun removed() {
        restorePlayerView()
        if (!closingForChild) closeAll(playSound = false)
        closingForChild = false
        super.removed()
    }

    override fun isPauseScreen(): Boolean = SaoOption.GUI_PAUSE()

    private fun buildOriginalTree() {
        val client = requireNotNull(minecraft)

        val profile = icon(SaoIcon.PROFILE, "sao.element.profile")
        profile.children += inventoryCategory(SaoItemCategory.EQUIPMENT)
        profile.children += inventoryCategory(SaoItemCategory.ITEMS)
        profile.children += skillsMenu()
        profile.children += LegacyMenuNode(SaoIcon.CRAFTING, Component.translatable("guiCrafting"), true,
            loadChildren = { (if (SaoMenuInventory.isCrafting) listOf(label(SaoIcon.CANCEL, "mcui.crafting.cancel") {
                SaoMenuInventory.cancelCraft()
            }) else emptyList()) + SaoMenuInventory.recipeGroups().map { group ->
                val groupIcon = group.recipes.first().value.getResultItem(client.level!!.registryAccess())
                LegacyMenuNode(SaoIcon.CRAFTING, Component.translatable(group.translation), true, item = groupIcon,
                    loadChildren = { group.recipes.map { recipe ->
                        val result = recipe.value.getResultItem(client.level!!.registryAccess())
                        LegacyMenuNode(SaoIcon.CRAFTING, result.hoverName, true, item = result,
                            action = { SaoMenuInventory.craft(this, recipe) })
                    } })
            }.ifEmpty { listOf(label(SaoIcon.CRAFTING, "gui.empty", false)) } })
        profile.children += LegacyMenuNode(SaoIcon.PROFILE, null, listed = false, profileContent = true)

        val social = icon(SaoIcon.SOCIAL, "sao.element.social")
        social.children += label(SaoIcon.GUILD, "sao.element.guild", enabled = SaoMcClient.serverSideLoaded)
        social.children += partyMenu()
        social.children += friendMenu()

        // The 1.12.2 registry defines MESSAGE as an intentionally empty top-level container.
        val message = icon(SaoIcon.MESSAGE, "sao.element.message")
        val navigation = icon(SaoIcon.NAVIGATION, "sao.element.navigation")
        navigation.children += questMenu()
        navigation.children += recipeMenu()

        val settings = icon(SaoIcon.SETTINGS, "sao.element.settings")
        val options = label(SaoIcon.OPTION, "sao.element.options")
        options.children += label(SaoIcon.OPTION, "guiOptions") {
            openChildScreen(OptionsScreen(this, client.options))
        }
        SaoOptionCategory.entries.filter { it.parent == null }.forEach {
            options.children += optionCategory(it)
        }
        settings.children += options
        settings.children += label(SaoIcon.HELP, "sao.element.menu") {
            restorePlayerView()
            closingForChild = true
            SaoScreenRouter.openVanillaPause(client)
        }
        settings.children += label(SaoIcon.LOGOUT, "sao.element.logout", enabled = client.player != null && SaoOption.LOGOUT()) {
            restorePlayerView()
            disconnectFromWorld(client)
        }

        topLevel += listOf(profile, social, message, navigation, settings)
        topLevel.forEachIndexed { index, node ->
            node.topIndex = index
            assignParents(node, null)
        }
    }

    private fun skillsMenu(): LegacyMenuNode = LegacyMenuNode(
        SaoIcon.SKILLS, Component.translatable("sao.element.skills"), true,
        loadChildren = {
            val client = requireNotNull(minecraft)
            val player = client.player
            listOf(
                LegacyMenuNode(SaoIcon.SKILLS, Component.translatable("skillSprinting"), true,
                    selected = { client.player?.isSprinting == true }, action = {
                        client.player?.let { it.isSprinting = !it.isSprinting }
                    }),
                LegacyMenuNode(SaoIcon.SKILLS, Component.translatable("skillSneaking"), true,
                    selected = { client.player?.isShiftKeyDown == true }, action = {
                        client.player?.let { it.setShiftKeyDown(!it.isShiftKeyDown) }
                    }),
                LegacyMenuNode(SaoIcon.CRAFTING, Component.translatable("skillCrafting"), player != null,
                    action = { player?.let { openChildScreen(InventoryScreen(it)) } }),
            )
        },
    )

    private fun questMenu(): LegacyMenuNode = LegacyMenuNode(SaoIcon.QUEST, Component.translatable("sao.element.quest"), true,
        loadChildren = {
            val manager = minecraft?.connection?.advancements
            val roots = manager?.tree?.roots()?.filter { it.advancement().display().isPresent }.orEmpty()
            roots.map { root ->
                val display = root.advancement().display().get()
                val done = (manager as be.bluexin.mcui.mixin.client.ClientAdvancementsAccessor).`mcui$getProgress`()[root.holder()]?.isDone == true
                if (!done) advancementRow(root, roots)
                else LegacyMenuNode(SaoIcon.QUEST, display.title, true, item = display.icon, loadChildren = {
                    listOf(true, false).map { completed ->
                        LegacyMenuNode(SaoIcon.QUEST, Component.translatable(if (completed) "sao.element.quest.completed" else "sao.element.quest.inProgress"), true,
                            loadChildren = {
                                val entries = root.children().filter { child ->
                                    child.advancement().display().isPresent &&
                                        ((manager as be.bluexin.mcui.mixin.client.ClientAdvancementsAccessor).`mcui$getProgress`()[child.holder()]?.isDone == true) == completed
                                }
                                entries.map { advancementRow(it, entries) }.ifEmpty { listOf(label(SaoIcon.QUEST, "gui.empty", false)) }
                            })
                    }
                })
            }.ifEmpty { listOf(label(SaoIcon.QUEST, "gui.empty", false)) }
        })

    private fun advancementRow(node: net.minecraft.advancements.AdvancementNode,
        siblings: List<net.minecraft.advancements.AdvancementNode>): LegacyMenuNode {
        val display = node.advancement().display().get()
        return LegacyMenuNode(SaoIcon.QUEST, display.title, true, item = display.icon,
            action = { showAdvancement(node, siblings) })
    }

    private fun showAdvancement(node: net.minecraft.advancements.AdvancementNode,
        siblings: List<net.minecraft.advancements.AdvancementNode>) {
        val display = node.advancement().display().get()
        val manager = minecraft?.connection?.advancements ?: return
        val progress = (manager as be.bluexin.mcui.mixin.client.ClientAdvancementsAccessor).`mcui$getProgress`()[node.holder()]
        val lines = listOf(display.description) + node.advancement().requirements().requirements().map { group ->
            Component.literal(group.joinToString(" / ") { criterion ->
                (if (progress?.getCriterion(criterion)?.isDone == true) "✓ " else "□ ") + criterion
            })
        }
        fun navigate(delta: Int) {
            showAdvancement(siblings[Math.floorMod(siblings.indexOf(node) + delta, siblings.size)], siblings)
        }
        openChildScreen(LegacyPopupScreen(this, display.title, lines, Component.empty(), listOf(
            PopupButton(SaoIcon.QUEST, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER, label = "<--", action = { navigate(-1) }),
            PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER, null),
            PopupButton(SaoIcon.QUEST, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER, label = "-->", action = { navigate(1) }),
        )))
    }

    private fun recipeMenu(): LegacyMenuNode = LegacyMenuNode(SaoIcon.CRAFTING,
        Component.translatable("sao.element.recipes"), true, loadChildren = {
            listOf(true, false).map { unlocked ->
                LegacyMenuNode(SaoIcon.CRAFTING, Component.translatable(if (unlocked) "sao.element.recipes.unlocked" else "sao.element.recipes.locked"), true,
                    loadChildren = {
                        val player = minecraft?.player
                        val level = minecraft?.level
                        if (player == null || level == null) emptyList()
                        else level.recipeManager.recipes.filter { player.recipeBook.contains(it) == unlocked }.mapNotNull { recipe ->
                            val result = recipe.value.getResultItem(player.registryAccess())
                            if (result.isEmpty) null else LegacyMenuNode(SaoIcon.CRAFTING, result.hoverName, true, item = result,
                                action = { openChildScreen(LegacyPopupScreen(this, result.hoverName,
                                    result.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(level), player, net.minecraft.world.item.TooltipFlag.NORMAL),
                                    Component.empty(), listOf(PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER, null)))) })
                        }.sortedBy { it.label?.string }.ifEmpty { listOf(label(SaoIcon.CRAFTING, "gui.empty", false)) }
                    })
            }
        })

    private fun friendMenu(): LegacyMenuNode = LegacyMenuNode(SaoIcon.FRIEND,
        Component.translatable("sao.element.friends"), true, loadChildren = {
            val client = requireNotNull(minecraft)
            val saved = SaoFriends.store.friends
            val add = LegacyMenuNode(SaoIcon.INVITE, Component.translatable("mcui.friends.add"), true,
                loadChildren = {
                    client.connection?.onlinePlayers.orEmpty().filter { candidate ->
                        candidate.profile.id != client.player?.uuid && saved.none { it.uuid == candidate.profile.id.toString() }
                    }.sortedBy { it.profile.name.lowercase() }.map { candidate ->
                        literalLabel(SaoIcon.PROFILE, candidate.profile.name) {
                            inspectPlayer(PlayerInfo(candidate.profile), "mcui.friends.add") {
                                SaoFriends.change { add(candidate.profile.id, candidate.profile.name) }
                            }
                        }
                    }.ifEmpty { listOf(label(SaoIcon.FRIEND, "gui.empty", false)) }
                })
            fun friendRow(friend: be.bluexin.mcui.config.SaoFriend) = literalLabel(SaoIcon.FRIEND, friend.name) {
                    inspectPlayer(PlayerInfo(java.util.UUID.fromString(friend.uuid), friend.name), "mcui.friends.remove") {
                        SaoFriends.change { remove(java.util.UUID.fromString(friend.uuid)) }
                    }
            }
            val (online, offline) = saved.sortedBy { it.name.lowercase() }.partition {
                client.connection?.getPlayerInfo(java.util.UUID.fromString(it.uuid)) != null
            }
            val offlineNode = label(SaoIcon.LOGOUT, "sao.element.offline_friends")
            offlineNode.children += offline.map(::friendRow).ifEmpty { listOf(label(SaoIcon.FRIEND, "gui.empty", false)) }
            listOf(add, offlineNode) + online.map(::friendRow)
        })

    private fun inspectPlayer(info: PlayerInfo, actionTitle: String? = null, friendAction: (() -> Unit)? = null) {
        val client = requireNotNull(minecraft)
        val player = client.player ?: return
        val inspected = client.level?.getPlayerByUUID(info.uuid)
        val lines = inspected?.let(SaoProfileStats::lines) ?: listOf(Component.translatable("mcui.player.unknown"))
        val buttons = mutableListOf(PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER,
            if (friendAction == null) null else {
                { friendAction(); client.setScreen(this) }
            }))
        if (friendAction != null) buttons += PopupButton(SaoIcon.CANCEL, LegacySaoMetrics.CANCEL, LegacySaoMetrics.CANCEL_HOVER, null)
        val party = player.getPartyCapability().partyData
        if (SaoMcClient.serverSideLoaded && (party == null || party.isLeader(player)) && info.uuid != player.uuid &&
            client.connection?.getPlayerInfo(info.uuid) != null && party?.isMember(info) != true && party?.isInvited(info) != true) {
            buttons += PopupButton(SaoIcon.PARTY, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER) {
                partyAction { Type.INVITE.updateServer(info, PartyType.MAIN) }
                client.setScreen(this)
            }
        }
        openChildScreen(LegacyPopupScreen(this, Component.literal(info.username), lines,
            actionTitle?.let(Component::translatable) ?: Component.empty(), buttons))
    }

    private fun partyMenu(): LegacyMenuNode {
        val client = minecraft ?: return label(SaoIcon.PARTY, "sao.element.party", enabled = false)
        val player = client.player ?: return label(SaoIcon.PARTY, "sao.element.party", enabled = false)
        val serverAvailable = SaoMcClient.serverSideLoaded
        val node = LegacyMenuNode(
            SaoIcon.PARTY,
            Component.translatable("sao.element.party"),
            listed = true,
            enabled = serverAvailable,
            description = if (serverAvailable) null else Component.translatable("mcui.party.server_required"),
        )
        if (!serverAvailable) return node

        val capability = player.getPartyCapability()
        val party = capability.partyData
        if (party == null || party.isLeader(player)) {
            val invite = label(SaoIcon.INVITE, "sao.party.invite")
            client.connection?.onlinePlayers
                ?.asSequence()
                ?.filterNot { it.profile.id == player.uuid }
                ?.map { PlayerInfo(it.profile) }
                ?.filterNot { candidate -> party?.isMember(candidate) == true || party?.isInvited(candidate) == true }
                ?.sortedBy { it.username.lowercase() }
                ?.forEach { candidate ->
                    invite.children += literalLabel(SaoIcon.PROFILE, candidate.username) {
                        partyAction { Type.INVITE.updateServer(candidate, PartyType.MAIN) }
                    }
                }
            node.children += invite
        }

        if (party != null) {
            party.getMembers().filterNot { it.uuid == player.uuid }.forEach { member ->
                val invited = party.isInvited(member)
                val memberNode = literalLabel(
                    SaoIcon.PROFILE,
                    if (invited) Component.translatable("sao.party.player_invited", member.username).string else member.username,
                )
                memberNode.children += label(SaoIcon.HELP, "sao.element.inspect") { inspectPlayer(member) }
                if (party.isLeader(player)) {
                    memberNode.children += LegacyMenuNode(
                        SaoIcon.CANCEL,
                        Component.translatable(if (invited) "sao.party.cancel" else "sao.party.kick"),
                        listed = true,
                        action = {
                            partyAction {
                                if (invited) Type.CANCELINVITE.updateServer(member, PartyType.MAIN)
                                else Type.KICK.updateServer(member, PartyType.MAIN)
                            }
                        },
                    )
                }
                node.children += memberNode
            }
            party.getInvited().filterNot { it.uuid == player.uuid }.forEach { invited ->
                if (node.children.none { it.label?.string?.contains(invited.username, ignoreCase = true) == true }) {
                    node.children += literalLabel(
                        SaoIcon.PROFILE,
                        Component.translatable("sao.party.player_invited", invited.username).string,
                    )
                }
            }
            if (party.size > 1) {
                node.children += label(SaoIcon.CANCEL, "sao.party.leave") {
                    partyAction { Type.LEAVE.updateServer(PlayerInfo(player), PartyType.MAIN) }
                }
            }
        }

        capability.inviteData.sortedBy { it.leaderInfo.username.lowercase() }.forEach { invitation ->
            val invitationNode = LegacyMenuNode(
                SaoIcon.PARTY,
                Component.translatable("sao.party.invited", invitation.leaderInfo.username),
                listed = true,
            )
            invitationNode.children += label(SaoIcon.CONFIRM, "sao.misc.accept") {
                partyAction { Type.ACCEPTINVITE.updateServer(player.uuid, PartyType.INVITE) }
            }
            invitationNode.children += label(SaoIcon.CANCEL, "sao.misc.decline") {
                partyAction { Type.CANCELINVITE.updateServer(player.uuid, PartyType.INVITE) }
            }
            node.children += invitationNode
        }
        return node
    }

    private fun inventoryCategory(category: SaoItemCategory): LegacyMenuNode = LegacyMenuNode(
        category.icon, Component.translatable(category.translation), true,
        loadChildren = {
            val subcategories = SaoItemCategory.entries.filter { it.parent == category }
            if (subcategories.isNotEmpty()) subcategories.map(::inventoryCategory)
            else SaoMenuInventory.items(category).map { (slot, stack) ->
                LegacyMenuNode(category.icon, stack.hoverName.copy().append(" ×${stack.count}"), true,
                    item = stack, action = { SaoMenuInventory.inspect(this, slot, stack, category) })
            }.ifEmpty { listOf(label(category.icon, "gui.empty", false)) }
        },
    )

    private fun refreshPartyMenuIfNeeded() {
        val signature = currentPartySignature()
        if (signature == partySignature) return
        partySignature = signature
        val social = topLevel.firstOrNull { it.icon == SaoIcon.SOCIAL } ?: return
        val index = social.children.indexOfFirst { it.icon == SaoIcon.PARTY }
        if (index < 0) return
        val old = social.children[index]
        val replacement = partyMenu().also {
            it.open = old.open
            it.openedAt = System.nanoTime()
            it.scroll = old.scroll
        }
        social.children[index] = replacement
        assignParents(replacement, social)
        normalizeLabelWidths(topLevel)
    }

    private fun currentPartySignature(): String {
        val player = minecraft?.player ?: return "no-player:${SaoMcClient.serverSideLoaded}"
        if (!SaoMcClient.serverSideLoaded) return "server-unavailable"
        val capability = runCatching { player.getPartyCapability() }.getOrNull() ?: return "state-unavailable"
        val party = capability.partyData
        return buildString {
            append(party?.leaderInfo?.uuid).append('|')
            party?.getMembers()?.sortedBy { it.uuid.toString() }?.forEach { append(it.uuid).append(':').append(it.isOnline).append(',') }
            append('|')
            party?.getInvited()?.sortedBy { it.uuid.toString() }?.forEach { append(it.uuid).append(',') }
            append('|')
            capability.inviteData.sortedBy { it.leaderInfo.uuid.toString() }.forEach { append(it.leaderInfo.uuid).append(',') }
        }
    }

    private fun partyAction(action: () -> Unit) {
        action()
        SaoSounds.play(SaoSound.CONFIRM)
    }

    private fun literalLabel(icon: SaoIcon, text: String, action: (() -> Unit)? = null) =
        LegacyMenuNode(icon, Component.literal(text), listed = true, action = action)

    private fun optionCategory(category: SaoOptionCategory): LegacyMenuNode {
        val node = label(SaoIcon.OPTION, category.translation)
        if (category == SaoOptionCategory.THEME) {
            MCUIThemes.manager.snapshot.themes.values.sortedBy { it.id.toString() }.forEach { theme ->
                node.children += LegacyMenuNode(
                    SaoIcon.OPTION, Component.literal(theme.metadata.name ?: theme.id.toString()), listed = true,
                    selected = { !SaoOption.VANILLA_UI() && MCUIThemes.manager.activeTheme.id == theme.id },
                    action = {
                        updateOptions {
                            SaoOptions.store.selectTheme(theme.id.toString())
                            MCUIThemes.manager.select(theme.id)
                        }
                    },
                )
            }
        }
        SaoOptionCategory.entries.filter { it.parent == category }.forEach { node.children += optionCategory(it) }
        SaoOption.entries.filter { it.category == category }.forEach { option ->
            node.children += LegacyMenuNode(
                SaoIcon.OPTION, Component.translatable(option.translation), listed = true,
                enabled = option in WIRED_OPTIONS,
                description = Component.translatable(if (option in WIRED_OPTIONS) "${option.translation}.desc" else "mcui.option.pending"),
                selected = { option() },
                action = { updateOptions { SaoOptions.store.set(option, !option()) } },
            )
        }
        return node
    }

    private fun updateOptions(change: () -> Unit) {
        runCatching(change).onFailure {
            be.bluexin.mcui.Constants.LOG.error("Could not save SAO settings", it)
            openChildScreen(LegacyPopupScreen(this, Component.translatable("mcui.option.save_failed"),
                listOf(Component.literal(it.message ?: it.javaClass.simpleName)), Component.empty(),
                listOf(PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER, null))))
        }
        if (!SaoOption.UI_MOVEMENT()) {
            capturedPlayer?.let { it.yRot = capturedYaw; it.xRot = capturedPitch }
        }
    }

    private fun icon(icon: SaoIcon, translation: String, enabled: Boolean = true) = LegacyMenuNode(
        icon, Component.translatable(translation), listed = true, enabled = enabled, topLevel = true,
    )

    private fun label(
        icon: SaoIcon,
        translation: String,
        enabled: Boolean = true,
        action: (() -> Unit)? = null,
    ) = LegacyMenuNode(icon, Component.translatable(translation), listed = true, enabled = enabled, action = action)

    private fun assignParents(node: LegacyMenuNode, parent: LegacyMenuNode?) {
        node.parent = parent
        node.children.forEach { assignParents(it, node) }
    }

    private fun normalizeLabelWidths(nodes: List<LegacyMenuNode>) {
        nodes.forEach { parent ->
            val labels = parent.children.filter { it.listed && !it.topLevel }
            val widest = labels.maxOfOrNull { child ->
                val textWidth = child.label?.let(font::width) ?: 0
                max(LegacySaoMetrics.LABEL_MIN_WIDTH, LegacySaoMetrics.LABEL_WIDTH_TEXT_PADDING + textWidth)
            } ?: LegacySaoMetrics.LABEL_MIN_WIDTH
            labels.forEach { it.labelWidth = widest }
            normalizeLabelWidths(parent.children)
        }
    }

    private fun openNode(node: LegacyMenuNode, playSound: Boolean) {
        node.loadChildren?.let { load ->
            node.children.clear()
            node.children += load()
            assignParents(node, node.parent)
            normalizeLabelWidths(topLevel)
        }
        node.parent?.children?.filter { it !== node && it.open }?.forEach { closeNode(it, playSound = false) }
        if (node.topLevel) topLevel.filter { it !== node && it.open }.forEach { closeNode(it, playSound = false) }
        node.open = true
        node.openedAt = System.nanoTime()
        node.scroll = -3
        if (!node.topLevel) {
            node.shiftApplied = node.displayWidth.toDouble()
            setMovementDestination(movementToX - node.shiftApplied)
        } else {
            // CategoryButton.open() called tlParent.move(Vec2d.ZERO), returning
            // any mouse-displaced CoreGUI root toward its destination.
            setMovementDestination(movementToX)
        }
        if (playSound) SaoSounds.play(SaoSound.MENU_POPUP)
    }

    private fun closeNode(node: LegacyMenuNode, playSound: Boolean) {
        node.children.filter { it.open }.forEach { closeNode(it, playSound = false) }
        node.open = false
        node.openedAt = 0L
        node.scroll = -3
        if (node.shiftApplied != 0.0) {
            setMovementDestination(movementToX + node.shiftApplied)
            node.shiftApplied = 0.0
        }
        if (node.topLevel) setMovementDestination(movementToX)
        if (playSound) SaoSounds.play(SaoSound.DIALOG_CLOSE)
    }

    private fun closeAll(playSound: Boolean) {
        topLevel.filter { it.open }.forEach { closeNode(it, playSound) }
    }

    private fun deepestOpenParent(): LegacyMenuNode? {
        var current = topLevel.firstOrNull { it.open } ?: return null
        while (true) current = current.children.firstOrNull { it.open } ?: return current
    }

    private fun setMovementDestination(destination: Double) {
        updateMovement()
        // CoreGUI.pos included UI-movement offsets. move() animated from that
        // actual position back to destination, so fold the offsets into the
        // animation's starting point before clearing them.
        movementX += parallaxX
        movementY += parallaxY
        parallaxX = 0.0
        parallaxY = 0.0
        movementFromX = movementX
        movementFromY = movementY
        movementToX = destination
        movementToY = baseRootY
        movementStartedAt = System.nanoTime()
        movementAnimating = true
    }

    private fun updateMovement() {
        if (!movementAnimating) return
        val elapsed = (System.nanoTime() - movementStartedAt).coerceAtLeast(0L) / 1_000_000.0
        val progress = (elapsed / LegacySaoMetrics.MOVE_MILLIS).coerceIn(0.0, 1.0)
        val eased = cubicBezier(
            progress,
            LegacySaoMetrics.MOVE_EASING_X1,
            LegacySaoMetrics.MOVE_EASING_Y1,
            LegacySaoMetrics.MOVE_EASING_X2,
            LegacySaoMetrics.MOVE_EASING_Y2,
        )
        movementX = movementFromX + (movementToX - movementFromX) * eased
        movementY = movementFromY + (movementToY - movementFromY) * eased
        if (progress >= 1.0) {
            movementX = movementToX
            movementY = movementToY
            movementAnimating = false
        }
    }

    private fun renderTree(
        graphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double,
        includeProfile: Boolean,
    ) {
        val entries = collectEntries(System.nanoTime())
        for (pass in RenderPass.entries) entries.forEach { entry ->
            if (!includeProfile && entry.node.profileContent) return@forEach
            when (pass) {
                RenderPass.BACKGROUND -> drawBackground(graphics, entry, mouseX, mouseY)
                RenderPass.CONTENT -> drawContent(graphics, entry, mouseX, mouseY)
                RenderPass.FOREGROUND -> if (SaoOption.MOUSE_OVER_EFFECT() && entry.contains(mouseX, mouseY)) {
                    entry.node.description?.let { graphics.renderTooltip(font, it, mouseX.toInt(), mouseY.toInt()) }
                }
            }
        }
    }

    private fun collectEntries(now: Long): MutableList<DrawEntry> {
        val result = mutableListOf<DrawEntry>()
        // Old GL translations accepted doubles, but Minecraft's bitmap glyphs
        // become unreadably soft when a modern pose remains between GUI pixels.
        // Snap the rendered and interactive tree together after interpolation.
        val rootX = (movementX + parallaxX).roundToInt().toDouble()
        val rootY = (movementY + parallaxY).roundToInt().toDouble()
        val entryProgress = menuEntryProgress(now)
        val selectedTop = topLevel.firstOrNull { it.open }
        topLevel.forEach { node ->
            val focusAlpha = if (selectedTop == null || selectedTop === node) 1f else LegacySaoMetrics.UNFOCUSED_ALPHA
            val entryY = rootY + node.topIndex * LegacySaoMetrics.TOP_LEVEL_SPACING * entryProgress
            collectNode(node, rootX, entryY.roundToInt().toDouble(), focusAlpha, now, result)
        }
        return result
    }

    /** Restores the 1.12 menu-entry position animation removed during the 1.16 rewrite. */
    private fun menuEntryProgress(now: Long): Double {
        val elapsed = (now - menuOpenedAt).coerceAtLeast(0L) / 1_000_000.0
        val progress = (elapsed / LegacySaoMetrics.MENU_ENTRY_MILLIS).coerceIn(0.0, 1.0)
        return cubicBezier(
            progress,
            LegacySaoMetrics.MOVE_EASING_X1,
            LegacySaoMetrics.MOVE_EASING_Y1,
            LegacySaoMetrics.MOVE_EASING_X2,
            LegacySaoMetrics.MOVE_EASING_Y2,
        )
    }

    private fun collectNode(
        node: LegacyMenuNode,
        x: Double,
        y: Double,
        inheritedAlpha: Float,
        now: Long,
        result: MutableList<DrawEntry>,
    ) {
        result += DrawEntry(node, x, y, inheritedAlpha)
        if (!node.open) return

        val listed = orderedVisibleChildren(node)
        val revealedCount = if (listed.isEmpty()) 0 else {
            (1L + ((now - node.openedAt).coerceAtLeast(0L) / 1_000_000L) / LegacySaoMetrics.CHILD_REVEAL_MILLIS)
                .toInt().coerceAtMost(listed.size)
        }
        val revealed = listed.take(revealedCount)
        val c = revealed.size.coerceAtMost(LegacySaoMetrics.MAX_VISIBLE_CHILDREN)
        val centering = ((c + c % 2 - 2) * LegacySaoMetrics.CHILD_Y_SPACING) / 2.0
        var childY = y - centering
        revealed.forEachIndexed { index, child ->
            val revealAt = node.openedAt + index * LegacySaoMetrics.CHILD_REVEAL_MILLIS * 1_000_000L
            val fade = ((now - revealAt).coerceAtLeast(0L) / 1_000_000.0 / LegacySaoMetrics.LABEL_FADE_MILLIS)
                .toFloat().coerceIn(0f, 1f)
            val edge = if (c == LegacySaoMetrics.MAX_VISIBLE_CHILDREN && (index == 0 || index == c - 1)) {
                LegacySaoMetrics.UNFOCUSED_ALPHA
            } else 1f
            // IconElement.transparency: a child is only the focus after its own
            // CategoryButton has opened; otherwise its opacity is divided by two.
            val focus = if (child.open) 1f else LegacySaoMetrics.UNFOCUSED_ALPHA
            collectNode(child, x + node.childrenXOffset, childY, focus * fade * edge, now, result)
            childY += LegacySaoMetrics.CHILD_Y_SPACING
        }

        if (revealedCount >= listed.size.coerceAtMost(LegacySaoMetrics.MAX_VISIBLE_CHILDREN)) {
            node.children.filter { !it.listed }.forEach { child ->
                // IconElement.drawChildren() left its listed-child matrix
                // translations applied before drawing non-listed elements.
                val otherX = x + node.childrenXOffset
                val otherY = y - centering + c * LegacySaoMetrics.CHILD_Y_SPACING
                collectNode(
                    child,
                    otherX,
                    otherY,
                    if (child.open) 1f else LegacySaoMetrics.UNFOCUSED_ALPHA,
                    now,
                    result,
                )
            }
        }
    }

    private fun orderedVisibleChildren(parent: LegacyMenuNode): List<LegacyMenuNode> {
        val children = parent.children.filter { it.listed }
        if (children.size < LegacySaoMetrics.MAX_VISIBLE_CHILDREN) return children
        val highlighted = children.indexOfFirst { it.open }
        val start = if (highlighted >= 0) {
            floorMod(highlighted - LegacySaoMetrics.MAX_VISIBLE_CHILDREN / 2, children.size)
        } else floorMod(parent.scroll, children.size)
        return List(LegacySaoMetrics.MAX_VISIBLE_CHILDREN) { children[(start + it) % children.size] }
    }

    private fun drawBackground(graphics: GuiGraphics, entry: DrawEntry, mouseX: Double, mouseY: Double) {
        val node = entry.node
        if (node.profileContent) {
            drawProfileBackground(graphics, entry, mouseY)
            return
        }
        val background = stateBackground(node, SaoOption.MOUSE_OVER_EFFECT() && entry.contains(mouseX, mouseY))
        setColor(graphics, background, entry.alpha)
        if (node.topLevel) {
            graphics.blit(
                LEGACY_GUI, entry.x.roundToInt(), entry.y.roundToInt(),
                LegacySaoMetrics.ICON_SIZE, LegacySaoMetrics.ICON_SIZE,
                LegacySaoMetrics.ICON_BACKGROUND_U.toFloat(),
                LegacySaoMetrics.ICON_BACKGROUND_V.toFloat(),
                LegacySaoMetrics.ICON_SIZE,
                LegacySaoMetrics.ICON_SIZE,
                LegacySaoMetrics.LEGACY_ATLAS_SIZE,
                LegacySaoMetrics.LEGACY_ATLAS_SIZE,
            )
        } else {
            graphics.blit(
                SaoUiStyle.SLOT, entry.x.roundToInt(), entry.y.roundToInt(),
                node.displayWidth, LegacySaoMetrics.LABEL_HEIGHT,
                LegacySaoMetrics.LABEL_BACKGROUND_U.toFloat(),
                LegacySaoMetrics.LABEL_BACKGROUND_V.toFloat(),
                LegacySaoMetrics.LABEL_MIN_WIDTH,
                LegacySaoMetrics.LABEL_HEIGHT,
                LegacySaoMetrics.LEGACY_ATLAS_SIZE,
                LegacySaoMetrics.LEGACY_ATLAS_SIZE,
            )
        }
        resetColor(graphics)
        if (SaoOption.MOUSE_OVER_EFFECT() && node.enabled && entry.contains(mouseX, mouseY)) {
            SaoUiStyle.renderMouseOverGlint(
                graphics,
                entry.x.roundToInt(),
                entry.y.roundToInt(),
                entry.node.hitWidth,
                entry.node.hitHeight,
                entry.alpha,
            )
        }
    }

    private fun drawContent(
        graphics: GuiGraphics,
        entry: DrawEntry,
        mouseX: Double,
        mouseY: Double,
    ) {
        val node = entry.node
        if (node.profileContent) {
            drawProfileEntity(graphics, entry)
            return
        }
        val hovered = SaoOption.MOUSE_OVER_EFFECT() && entry.contains(mouseX, mouseY)
        val textColor = stateText(node, hovered)
        if (node.item != null) {
            graphics.renderItem(node.item, entry.x.roundToInt() + 1, entry.y.roundToInt() + 1)
        } else SaoUiStyle.renderIcon(
            graphics, node.icon,
            entry.x.roundToInt() + LegacySaoMetrics.ICON_CONTENT_OFFSET,
            entry.y.roundToInt() + LegacySaoMetrics.ICON_CONTENT_OFFSET,
            LegacySaoMetrics.ICON_CONTENT_SIZE, textColor, entry.alpha,
        )
        if (!node.topLevel && node.label != null) {
            graphics.drawString(
                font, node.label,
                entry.x.roundToInt() + LegacySaoMetrics.LABEL_TEXT_X,
                entry.y.roundToInt() + (LegacySaoMetrics.LABEL_HEIGHT - 8) / 2,
                SaoUiStyle.multiplyAlpha(textColor, entry.alpha), SaoOption.TEXT_SHADOW() && (hovered || node.open || keyboardSelected === node),
            )
        }
    }

    private fun drawProfileBackground(
        graphics: GuiGraphics,
        entry: DrawEntry,
        mouseY: Double,
    ) {
        val player = minecraft?.player ?: return
        val x = (entry.x + LegacySaoMetrics.PROFILE_X).roundToInt()
        val y = (entry.y + LegacySaoMetrics.PROFILE_Y).roundToInt()
        val left = x + LegacySaoMetrics.PROFILE_WIDTH / 2 + LegacySaoMetrics.PROFILE_ENTITY_CENTER_X_OFFSET
        val top = y + LegacySaoMetrics.PROFILE_HEIGHT / 2

        // ProfileElement explicitly reset the GL color to opaque white, regardless of focus.
        graphics.setColor(1f, 1f, 1f, 1f)
        graphics.blit(
            SaoUiStyle.PROFILE_BACKGROUND,
            x,
            y,
            LegacySaoMetrics.PROFILE_WIDTH,
            LegacySaoMetrics.PROFILE_HEIGHT,
            // Sample the real 512px source region directly. This preserves the
            // ProfileElement crop without treating the PNG as a smaller atlas.
            0f,
            0f,
            LegacySaoMetrics.PROFILE_WIDTH * LegacySaoMetrics.PROFILE_TEXTURE_SCALE,
            LegacySaoMetrics.PROFILE_HEIGHT * LegacySaoMetrics.PROFILE_TEXTURE_SCALE,
            LegacySaoMetrics.PROFILE_TEXTURE_SIZE,
            LegacySaoMetrics.PROFILE_TEXTURE_SIZE,
        )
        graphics.setColor(1f, 1f, 1f, 1f)

        val shadowHeight = LegacySaoMetrics.PROFILE_ENTITY_SIZE / 2 + max(
            minOf((mouseY - y).toInt(), 0),
            -LegacySaoMetrics.PROFILE_ENTITY_SIZE / 2 + 2,
        )
        if (shadowHeight > 0) {
            graphics.setColor(1f, 1f, 1f, 1f)
            graphics.blit(
                LEGACY_GUI,
                left - LegacySaoMetrics.PROFILE_ENTITY_SIZE / 2,
                top - shadowHeight / 2,
                LegacySaoMetrics.PROFILE_ENTITY_SIZE,
                shadowHeight,
                LegacySaoMetrics.PROFILE_SHADOW_U.toFloat(),
                LegacySaoMetrics.PROFILE_SHADOW_V.toFloat(),
                LegacySaoMetrics.PROFILE_SHADOW_SOURCE_WIDTH,
                LegacySaoMetrics.PROFILE_SHADOW_SOURCE_HEIGHT,
                LegacySaoMetrics.LEGACY_ATLAS_SIZE,
                LegacySaoMetrics.LEGACY_ATLAS_SIZE,
            )
            graphics.setColor(1f, 1f, 1f, 1f)
        }
        val displayedNameLength = player.displayName?.string?.length ?: player.scoreboardName.length
        val legacyNameX = x + LegacySaoMetrics.PROFILE_NAME_X + displayedNameLength / 2
        graphics.drawString(
            font,
            player.scoreboardName,
            legacyNameX,
            y + LegacySaoMetrics.PROFILE_NAME_Y - font.lineHeight / 2,
            LegacySaoMetrics.POPUP_TEXT,
            false,
        )
        val stats = SaoProfileStats.lines(player)
        stats.forEachIndexed { index, line ->
            graphics.drawString(
                font,
                line,
                left - font.width(line) / 2,
                y + LegacySaoMetrics.PROFILE_STATS_Y + index * font.lineHeight,
                LegacySaoMetrics.DEFAULT_TEXT,
                false,
            )
        }
    }

    private fun drawProfileEntity(graphics: GuiGraphics, entry: DrawEntry) {
        val player = minecraft?.player ?: return
        val x = (entry.x + LegacySaoMetrics.PROFILE_X).roundToInt()
        val y = (entry.y + LegacySaoMetrics.PROFILE_Y).roundToInt()
        val left = x + LegacySaoMetrics.PROFILE_WIDTH / 2 + LegacySaoMetrics.PROFILE_ENTITY_CENTER_X_OFFSET
        val top = y + LegacySaoMetrics.PROFILE_HEIGHT / 2
        val displayedEntity = if (SaoOption.MOUNT_STAT_VIEW()) (player.rootVehicle as? net.minecraft.world.entity.LivingEntity) ?: player else player
        // 1.12.2 drawEntityOnScreen uses a FOOT baseline, not the modern bounding-box center.
        val yaw = kotlin.math.atan(width / 3.5f / 40f)
        val pitch = kotlin.math.atan(20f / 40f)
        val previous = floatArrayOf(displayedEntity.yBodyRot, displayedEntity.yRot, displayedEntity.xRot,
            displayedEntity.yHeadRotO, displayedEntity.yHeadRot)
        val tilt = org.joml.Quaternionf().rotationX(pitch * 20f * (Math.PI / 180).toFloat())
        try {
            displayedEntity.yBodyRot = 180f + yaw * 20f
            displayedEntity.yRot = 180f + yaw * 40f
            displayedEntity.xRot = -pitch * 20f
            displayedEntity.yHeadRotO = displayedEntity.yRot
            displayedEntity.yHeadRot = displayedEntity.yRot
            InventoryScreen.renderEntityInInventory(graphics, left.toFloat(), top.toFloat(),
                LegacySaoMetrics.PROFILE_ENTITY_SIZE.toFloat(), org.joml.Vector3f(),
                org.joml.Quaternionf().rotationZ(Math.PI.toFloat()).mul(tilt), tilt, displayedEntity)
        } finally {
            displayedEntity.yBodyRot = previous[0]; displayedEntity.yRot = previous[1]
            displayedEntity.xRot = previous[2]; displayedEntity.yHeadRotO = previous[3]
            displayedEntity.yHeadRot = previous[4]
        }
    }

    private fun stateBackground(node: LegacyMenuNode, hovered: Boolean): Int = when {
        !node.enabled -> LegacySaoMetrics.DISABLED_BACKGROUND
        hovered || node.open || node.selected() || keyboardSelected === node -> LegacySaoMetrics.HOVER_BACKGROUND
        else -> LegacySaoMetrics.DEFAULT_BACKGROUND
    }

    private fun stateText(node: LegacyMenuNode, hovered: Boolean): Int = when {
        !node.enabled || hovered || node.open || node.selected() || keyboardSelected === node -> LegacySaoMetrics.WHITE
        else -> LegacySaoMetrics.DEFAULT_TEXT
    }

    private fun openChildScreen(screen: Screen) {
        restorePlayerView()
        closingForChild = true
        minecraft?.setScreen(screen)
    }

    private fun disconnectFromWorld(client: Minecraft) {
        val localServer = client.isLocalServer
        val server = client.currentServer
        client.level?.disconnect()
        if (localServer) client.disconnect(GenericMessageScreen(Component.translatable("menu.savingLevel"))) else client.disconnect()
        val title = SaoTitleScreen()
        client.setScreen(if (localServer || server?.isRealm == true) title else JoinMultiplayerScreen(title))
    }

    private fun capturePlayerView() {
        val player = minecraft?.player ?: return
        capturedPlayer = player
        capturedYaw = player.yRot
        capturedPitch = player.xRot
    }

    private fun updatePlayerView(mouseX: Int, mouseY: Int) {
        if (!SaoOption.UI_MOVEMENT()) return
        val player = capturedPlayer ?: return
        if (minecraft?.player !== player) {
            restorePlayerView()
            capturePlayerView()
            return
        }
        player.yRot = capturedYaw + (mouseX - width / 2f) * LegacySaoMetrics.PLAYER_VIEW_MOUSE_MOVEMENT
        player.xRot = capturedPitch + (mouseY - height / 2f) * LegacySaoMetrics.PLAYER_VIEW_MOUSE_MOVEMENT
    }

    private fun restorePlayerView() {
        capturedPlayer?.let { player ->
            player.yRot = capturedYaw
            player.xRot = capturedPitch
        }
        capturedPlayer = null
    }

    private fun setColor(graphics: GuiGraphics, color: Int, alpha: Float) {
        graphics.setColor(
            (color ushr 16 and 0xFF) / 255f,
            (color ushr 8 and 0xFF) / 255f,
            (color and 0xFF) / 255f,
            (color ushr 24 and 0xFF) / 255f * alpha.coerceIn(0f, 1f),
        )
    }

    private fun resetColor(graphics: GuiGraphics) = graphics.setColor(1f, 1f, 1f, 1f)

    private data class DrawEntry(val node: LegacyMenuNode, val x: Double, val y: Double, val alpha: Float) {
        fun contains(mouseX: Double, mouseY: Double): Boolean =
            mouseX >= x && mouseX < x + node.hitWidth && mouseY >= y && mouseY < y + node.hitHeight
    }

    private enum class RenderPass { BACKGROUND, CONTENT, FOREGROUND }

    companion object {
        private val WIRED_OPTIONS = setOf(
            SaoOption.UI_ONLY, SaoOption.SPINNING_CRYSTALS, SaoOption.DEFAULT_DEBUG, SaoOption.AGGRO_SYSTEM,
            SaoOption.DEFAULT_INVENTORY, SaoOption.DEFAULT_DEATH_SCREEN, SaoOption.FORCE_HUD,
            SaoOption.LOGOUT, SaoOption.GUI_PAUSE, SaoOption.UI_MOVEMENT, SaoOption.VANILLA_UI,
            SaoOption.SMOOTH_HEALTH, SaoOption.REMOVE_HPXP, SaoOption.ALT_ABSORB_POS,
            SaoOption.ENEMY_ONSCREEN_HEALTH, SaoOption.DEFAULT_HOTBAR, SaoOption.HOR_HOTBAR,
            SaoOption.VER_HOTBAR, SaoOption.SOUND_EFFECTS, SaoOption.PARTICLES, SaoOption.MOUSE_OVER_EFFECT,
            SaoOption.MOUNT_STAT_VIEW, SaoOption.TEXT_SHADOW, SaoOption.HIDE_OFFLINE_PARTY, SaoOption.CUSTOM_FONT,
            SaoOption.RENDER_CROSSHAIRS, SaoOption.RENDER_ARMOR, SaoOption.RENDER_HOTBAR,
            SaoOption.RENDER_AIR, SaoOption.RENDER_POTION_ICONS, SaoOption.RENDER_HEALTH,
            SaoOption.RENDER_FOOD, SaoOption.RENDER_EXPERIENCE, SaoOption.RENDER_JUMPBAR, SaoOption.RENDER_HEALTHMOUNT,
        ) + SaoOption.entries.filter { it.category in setOf(SaoOptionCategory.ENTITY_HEALTH, SaoOptionCategory.CRYSTALS) }
        private val LEGACY_GUI = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("saoui", "textures/guiedt.png")

        private fun floorMod(value: Int, modulus: Int): Int = ((value % modulus) + modulus) % modulus

        private fun cubicBezier(x: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
            var low = 0.0
            var high = 1.0
            repeat(16) {
                val t = (low + high) * 0.5
                if (bezierCoordinate(t, x1, x2) < x) low = t else high = t
            }
            return bezierCoordinate((low + high) * 0.5, y1, y2)
        }

        private fun bezierCoordinate(t: Double, p1: Double, p2: Double): Double {
            val inverse = 1.0 - t
            return 3.0 * inverse * inverse * t * p1 + 3.0 * inverse * t * t * p2 + t * t * t
        }
    }
}

private class LegacyMenuNode(
    val icon: SaoIcon,
    val label: Component?,
    val listed: Boolean,
    val enabled: Boolean = true,
    val topLevel: Boolean = false,
    val profileContent: Boolean = false,
    val selected: () -> Boolean = { false },
    val description: Component? = null,
    val action: (() -> Unit)? = null,
    val item: net.minecraft.world.item.ItemStack? = null,
    val loadChildren: (() -> List<LegacyMenuNode>)? = null,
) {
    val children = mutableListOf<LegacyMenuNode>()
    var parent: LegacyMenuNode? = null
    var topIndex = 0
    var labelWidth = LegacySaoMetrics.LABEL_MIN_WIDTH
    var open = false
    var openedAt = 0L
    var scroll = -3
    var shiftApplied = 0.0

    val displayWidth: Int get() = if (topLevel) LegacySaoMetrics.ICON_SIZE else labelWidth
    val hitWidth: Int get() = if (topLevel) LegacySaoMetrics.ICON_BOUND else displayWidth
    val hitHeight: Int get() = if (topLevel) LegacySaoMetrics.ICON_BOUND else LegacySaoMetrics.LABEL_HEIGHT
    val childrenXOffset: Int
        get() = if (topLevel) LegacySaoMetrics.CHILD_X_OFFSET else displayWidth + LegacySaoMetrics.LABEL_CHILD_GAP

    fun isDescendantOf(candidate: LegacyMenuNode): Boolean {
        var current = parent
        while (current != null) {
            if (current === candidate) return true
            current = current.parent
        }
        return false
    }
}
