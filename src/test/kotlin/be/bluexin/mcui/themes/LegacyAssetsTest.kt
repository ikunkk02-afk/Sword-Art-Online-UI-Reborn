package be.bluexin.mcui.themes

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LegacyAssetsTest {
    @Test fun `all 74 original textures and sounds match the 1_12_2 branch byte for byte`() {
        val loader = javaClass.classLoader
        val manifest = assertNotNull(loader.getResourceAsStream("saoui-1.12.2-assets.sha256"))
            .bufferedReader().use { it.readLines() }.filter { it.isNotBlank() }
        assertEquals(74, manifest.size)
        manifest.forEach { line ->
            val (expected, path) = line.split("  ", limit = 2)
            val bytes = assertNotNull(loader.getResourceAsStream("assets/saoui/$path"), path).use { it.readBytes() }
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals(expected, actual, "Original resource changed: $path")
        }
    }
}
