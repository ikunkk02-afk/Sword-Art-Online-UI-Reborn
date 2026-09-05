package be.bluexin.mcui.render

import be.bluexin.mcui.screens.LegacySaoMetrics
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyProfileMetricsTest {
    @Test fun `four restored profile actions align the original panel pointer with the orb`() {
        val children = 4
        val centering = (children + children % 2 - 2) * LegacySaoMetrics.CHILD_Y_SPACING / 2
        val unlistedY = -centering + children * LegacySaoMetrics.CHILD_Y_SPACING
        val pointerY = unlistedY + LegacySaoMetrics.PROFILE_Y + 205.0 / LegacySaoMetrics.PROFILE_TEXTURE_SCALE
        assertEquals(LegacySaoMetrics.ICON_SIZE / 2.0, pointerY)
    }
}
