/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package be.bluexin.mcui.screens

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
        capturePlayerView()
        SaoSounds.play(SaoSound.ORB_DROPDOWN)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // CoreGUI rendered directly over the world; it had no menu backdrop panel.
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
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
        if (node.children.isNotEmpty()) {
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
        if (oldX != null && oldY != null) {
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
            if (selected.children.isNotEmpty()) {
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

    override fun isPauseScreen(): Boolean = false

    private fun buildOriginalTree() {
        val client = requireNotNull(minecraft)

        val profile = icon(SaoIcon.PROFILE, "sao.element.profile")
        profile.children += label(SaoIcon.SKILLS, "sao.element.skills", enabled = false)
        profile.children += LegacyMenuNode(SaoIcon.PROFILE, null, listed = false, profileContent = true)

        val social = icon(SaoIcon.SOCIAL, "sao.element.social")
        social.children += label(SaoIcon.GUILD, "sao.element.guild", enabled = false)
        social.children += label(SaoIcon.PARTY, "sao.element.party", enabled = false)
        social.children += label(SaoIcon.FRIEND, "sao.element.friends", enabled = false)

        val message = icon(SaoIcon.MESSAGE, "sao.element.message", enabled = false)
        val navigation = icon(SaoIcon.NAVIGATION, "sao.element.navigation", enabled = false)

        val settings = icon(SaoIcon.SETTINGS, "sao.element.settings")
        val options = label(SaoIcon.OPTION, "sao.element.options")
        options.children += label(SaoIcon.OPTION, "guiOptions") {
            openChildScreen(OptionsScreen(this, client.options))
        }
        settings.children += options
        settings.children += label(SaoIcon.HELP, "sao.element.menu") {
            restorePlayerView()
            closingForChild = true
            SaoScreenRouter.openVanillaPause(client)
        }
        settings.children += label(SaoIcon.LOGOUT, "sao.element.logout", enabled = client.player != null) {
            restorePlayerView()
            disconnectFromWorld(client)
        }

        topLevel += listOf(profile, social, message, navigation, settings)
        topLevel.forEachIndexed { index, node ->
            node.topIndex = index
            assignParents(node, null)
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
                RenderPass.FOREGROUND -> Unit
            }
        }
    }

    private fun collectEntries(now: Long): MutableList<DrawEntry> {
        val result = mutableListOf<DrawEntry>()
        val rootX = movementX + parallaxX
        val rootY = movementY + parallaxY
        val selectedTop = topLevel.firstOrNull { it.open }
        topLevel.forEach { node ->
            val focusAlpha = if (selectedTop == null || selectedTop === node) 1f else LegacySaoMetrics.UNFOCUSED_ALPHA
            collectNode(node, rootX, rootY + node.topIndex * LegacySaoMetrics.TOP_LEVEL_SPACING, focusAlpha, now, result)
        }
        return result
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
        val background = stateBackground(node, entry.contains(mouseX, mouseY))
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
        val hovered = entry.contains(mouseX, mouseY)
        val textColor = stateText(node, hovered, entry.alpha)
        SaoUiStyle.renderIcon(
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
                SaoUiStyle.multiplyAlpha(textColor, entry.alpha), hovered || node.open || keyboardSelected === node,
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

        graphics.setColor(1f, 1f, 1f, entry.alpha)
        graphics.blit(
            SaoUiStyle.PROFILE_BACKGROUND,
            x,
            y,
            LegacySaoMetrics.PROFILE_WIDTH,
            LegacySaoMetrics.PROFILE_HEIGHT,
            // ProfileElement used a 256×256 logical atlas. The PNG is a 2× source asset.
            0f,
            0f,
            LegacySaoMetrics.PROFILE_WIDTH,
            LegacySaoMetrics.PROFILE_HEIGHT,
            LegacySaoMetrics.LEGACY_ATLAS_SIZE,
            LegacySaoMetrics.LEGACY_ATLAS_SIZE,
        )
        graphics.setColor(1f, 1f, 1f, 1f)

        val shadowHeight = LegacySaoMetrics.PROFILE_ENTITY_SIZE / 2 + max(
            minOf((mouseY - y).toInt(), 0),
            -LegacySaoMetrics.PROFILE_ENTITY_SIZE / 2 + 2,
        )
        if (shadowHeight > 0) {
            graphics.setColor(1f, 1f, 1f, entry.alpha)
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
        val stats = listOf(
            Component.translatable("displayLvShort", player.experienceLevel),
            Component.literal("${player.health.roundToInt()} / ${player.maxHealth.roundToInt()}"),
            Component.literal(player.armorValue.toString()),
        )
        stats.forEachIndexed { index, line ->
            graphics.drawString(
                font,
                line,
                left - font.width(line) / 2,
                y + LegacySaoMetrics.PROFILE_STATS_Y + index * font.lineHeight,
                if (entry.alpha < 1f) LegacySaoMetrics.POPUP_TEXT else LegacySaoMetrics.DEFAULT_TEXT,
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
        val displayedEntity = (player.vehicle as? net.minecraft.world.entity.LivingEntity) ?: player
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            graphics,
            x,
            y,
            x + (left - x) * 2,
            y + LegacySaoMetrics.PROFILE_HEIGHT,
            LegacySaoMetrics.PROFILE_ENTITY_SIZE, 0.0625f,
            left - width / 3.5f,
            top - 20f,
            displayedEntity,
        )
    }

    private fun stateBackground(node: LegacyMenuNode, hovered: Boolean): Int = when {
        !node.enabled -> LegacySaoMetrics.DISABLED_BACKGROUND
        hovered || node.open || keyboardSelected === node -> LegacySaoMetrics.HOVER_BACKGROUND
        else -> LegacySaoMetrics.DEFAULT_BACKGROUND
    }

    private fun stateText(node: LegacyMenuNode, hovered: Boolean, alpha: Float): Int = when {
        !node.enabled || hovered || node.open || keyboardSelected === node ||
            alpha <= LegacySaoMetrics.UNFOCUSED_ALPHA -> LegacySaoMetrics.WHITE
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
        private val LEGACY_GUI = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("saoui", "textures/sao/gui.png")

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
    val action: (() -> Unit)? = null,
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
