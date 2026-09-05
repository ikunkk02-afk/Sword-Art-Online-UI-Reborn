/* Copyright (C) 2020-2021 Tencao
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package be.bluexin.mcui.screens

import com.tencao.saomclib.events.PartyEvent
import com.tencao.saomclib.events.PartyEvents
import com.tencao.saomclib.packets.PartyType
import com.tencao.saomclib.packets.Type
import com.tencao.saomclib.packets.to_server.updateServer
import com.tencao.saomclib.party.PlayerInfo
import com.tencao.saomclib.capabilities.getPartyCapability
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

/** Restores the original EventCore party notifications on Fabric's SAOMCLib event surface. */
object SaoPartyEvents {
    private var registered = false
    private val pendingInvitations = linkedMapOf<java.util.UUID, PartyEvent.Invited>()
    private var invitationPlayer: net.minecraft.client.player.LocalPlayer? = null

    fun tick() {
        val client = Minecraft.getInstance()
        val player = client.player
        if (player !== invitationPlayer) {
            pendingInvitations.clear()
            invitationPlayer = player
        }
        if (player == null || client.screen !is SaoIngameMenuScreen) return
        val live = player.getPartyCapability().inviteData.map { it.leaderInfo.uuid }.toSet()
        pendingInvitations.keys.retainAll(live)
        val next = pendingInvitations.entries.firstOrNull() ?: return
        pendingInvitations.remove(next.key)
        showInvitation(next.value, PlayerInfo(player))
    }

    fun register() {
        if (registered) return
        registered = true
        PartyEvents.EVENT.register { event ->
            Minecraft.getInstance().execute { handle(event) }
        }
    }

    private fun handle(event: PartyEvent) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val local = PlayerInfo(player)
        when (event) {
            is PartyEvent.Invited -> if (event.player == local) showInvitation(event, local)
            is PartyEvent.InviteCanceled -> if (event.player == local) {
                pendingInvitations.remove(event.partyData.leaderInfo.uuid)
                notify("notificationPartyInviteTimeoutTitle")
            }
            is PartyEvent.Join -> when {
                event.player == local -> notify(
                    "notificationPartyJoinedTitle",
                    "notificationPartyJoinedShortText",
                    event.partyData.leaderInfo.username,
                )
                event.partyData.isMember(local) -> notify(
                    "notificationPartyAddedTitle",
                    "notificationPartyAddedShortText",
                    event.player.username,
                )
            }
            is PartyEvent.Leave -> when {
                event.player == local -> notify(
                    "notificationPartyLeftTitle",
                    "notificationPartyLeftShortText",
                    event.partyData.leaderInfo.username,
                )
                event.partyData.isMember(local) -> notify(
                    "notificationPartyLeaveTitle",
                    "notificationPartyLeaveShortText",
                    event.player.username,
                )
            }
            is PartyEvent.Kicked -> when {
                event.player == local -> notify("notificationPartyLeftTitle")
                event.partyData.isMember(local) -> notify(
                    "notificationPartyLeaveTitle",
                    "notificationPartyLeaveShortText",
                    event.player.username,
                )
            }
            is PartyEvent.Disbanded -> if (event.partyData.isMember(local)) notify("notificationPartyDisbandTitle")
            is PartyEvent.LeaderChanged -> when {
                event.newLeader == local -> notify("notificationPartyLeaderTitle", "notificationPartyLeaderShortText")
                event.partyData.isMember(local) -> notify(
                    "notificationPartyNewLeaderTitle",
                    "notificationPartyNewLeaderShortText",
                    event.newLeader.username,
                )
            }
            is PartyEvent.Refresh -> Unit
        }
    }

    private fun showInvitation(event: PartyEvent.Invited, local: PlayerInfo) {
        val client = Minecraft.getInstance()
        val parent = client.screen as? SaoIngameMenuScreen
        if (parent == null) {
            invitationPlayer = client.player
            pendingInvitations[event.partyData.leaderInfo.uuid] = event
            notify("notificationPartyInviteTitle", "notificationPartyInviteShortText", event.partyData.leaderInfo.username)
            return
        }
        parent.preparePopupReturn()
        val memberLines = event.partyData.getMembers().map { Component.literal(it.username) }
        client.setScreen(
            LegacyPopupScreen(
                parent = parent,
                header = Component.translatable("guiPartyInviteTitle"),
                lines = listOf(Component.translatable("guiPartyInviteText", event.partyData.leaderInfo.username)) + memberLines,
                footer = Component.empty(),
                buttons = listOf(
                    PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER) {
                        if (client.player?.getPartyCapability()?.inviteData?.any { it.leaderInfo == event.partyData.leaderInfo } == true)
                            Type.ACCEPTINVITE.updateServer(local.uuid, PartyType.INVITE)
                        client.setScreen(parent)
                    },
                    PopupButton(SaoIcon.CANCEL, LegacySaoMetrics.CANCEL, LegacySaoMetrics.CANCEL_HOVER) {
                        Type.CANCELINVITE.updateServer(local.uuid, PartyType.INVITE)
                        client.setScreen(parent)
                    },
                ),
            ),
        )
    }

    private fun notify(titleKey: String, subtitleKey: String? = null, vararg args: Any) {
        SaoNotificationAlert.show(
            SaoIcon.PARTY,
            Component.translatable(titleKey),
            subtitleKey?.let { Component.translatable(it, *args) } ?: Component.empty(),
        )
    }
}
