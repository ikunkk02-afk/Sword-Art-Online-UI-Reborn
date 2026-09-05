package be.bluexin.mcui.render

import be.bluexin.mcui.fabric.client.hud.LegacyEntityHudSelection
import be.bluexin.mcui.fabric.client.hud.TargetEntitySnapshot
import net.minecraft.resources.ResourceLocation
import kotlin.test.*

class LegacyEntityHudSelectionTest {
    private fun entity(id: Int, hp: Float = 10f) = TargetEntitySnapshot(id, "entity$id", hp, 20f,
        ResourceLocation.withDefaultNamespace("zombie"), 1f, true)

    @Test fun `tracked entity is displayed even without nearby hostile entities`() {
        val target = entity(1)
        assertEquals(listOf(target), LegacyEntityHudSelection.select(target, emptyList()))
    }

    @Test fun `target does not consume one of five nearby rows and output is health ordered`() {
        val target = entity(9, 20f)
        val nearby = (1..7).map { entity(it, it.toFloat()) }
        assertEquals(listOf(1, 2, 3, 4, 5, 9), LegacyEntityHudSelection.select(target, listOf(target) + nearby).map { it.entityId })
    }

    @Test fun `duplicate dead and invalid health entries cannot create bad bars`() {
        val valid = entity(1)
        assertEquals(listOf(valid), LegacyEntityHudSelection.select(null, listOf(valid, valid,
            entity(2).copy(alive = false), entity(3).copy(maxHealth = 0f), entity(4, Float.NaN))))
    }
}
