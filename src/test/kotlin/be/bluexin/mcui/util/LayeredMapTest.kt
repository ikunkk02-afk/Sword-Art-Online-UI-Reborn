package be.bluexin.mcui.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayeredMapTest {
    @Test
    fun newestLayerShadowsOlderValues() {
        val map = LayeredMap<String, Int>()
        map += mapOf("shared" to 1, "base" to 2)
        map += mapOf("shared" to 3, "top" to 4)

        assertEquals(3, map["shared"])
        assertEquals(setOf("shared", "base", "top"), map.keys)
        assertEquals(3, map.size)
        assertTrue(map.containsValue(3))
        assertFalse(map.containsValue(1))

        map.pop()
        assertEquals(1, map["shared"])
        assertFalse("top" in map)
    }
}
