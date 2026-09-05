package be.bluexin.mcui.fabric.client.hud

/** Input nearby entities are distance-ordered by the provider, before health ordering. */
object LegacyEntityHudSelection {
    fun select(target: TargetEntitySnapshot?, nearby: List<TargetEntitySnapshot>): List<TargetEntitySnapshot> =
        (listOfNotNull(target) + nearby.filterNot { it.entityId == target?.entityId }.distinctBy { it.entityId }.take(5))
            .filter { it.alive && it.health.isFinite() && it.maxHealth.isFinite() && it.maxHealth > 0f }
            .sortedBy { it.health / it.maxHealth }
}
