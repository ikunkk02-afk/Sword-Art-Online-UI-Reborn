package be.bluexin.mcui.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaoOptionStoreTest {
    @Test fun `hotbar selection is exclusive and selecting it twice does not disable it`() {
        val store = SaoOptionStore()
        store.set(SaoOption.HOR_HOTBAR, true)
        store.set(SaoOption.HOR_HOTBAR, false)
        assertTrue(store[SaoOption.HOR_HOTBAR])
        assertFalse(store[SaoOption.DEFAULT_HOTBAR])
        assertFalse(store[SaoOption.VER_HOTBAR])
        store.set(SaoOption.DEFAULT_HOTBAR, true)
        assertTrue(store[SaoOption.DEFAULT_HOTBAR])
        assertFalse(store[SaoOption.HOR_HOTBAR])
    }

    @Test fun `settings persist theme selection and unrelated switches across restart`() {
        val dir = Files.createTempDirectory("mcui-options-test")
        val file = dir.resolve("options.json")
        try {
            val store = SaoOptionStore(file)
            store.set(SaoOption.UI_MOVEMENT, false)
            store.set(SaoOption.HOR_HOTBAR, true)
            store.set(SaoOption.VANILLA_UI, true)
            store.selectTheme("test:resource_pack")
            val restarted = SaoOptionStore(file)
            restarted.load()
            assertEquals("test:resource_pack", restarted.settings.theme)
            assertFalse(restarted[SaoOption.UI_MOVEMENT])
            assertFalse(restarted[SaoOption.VANILLA_UI])
            assertTrue(restarted[SaoOption.HOR_HOTBAR])
            assertFalse(restarted[SaoOption.VER_HOTBAR])
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(dir) }
    }

    @Test fun `legacy always-show key migrates to the original force-hud option`() {
        val dir = Files.createTempDirectory("mcui-options-migration-test")
        val file = dir.resolve("options.json")
        try {
            Files.writeString(file, """{"options":{"ALWAYS_SHOW":false}}""")
            val store = SaoOptionStore(file)
            store.load()
            assertFalse(store[SaoOption.FORCE_HUD])
            assertFalse("ALWAYS_SHOW" in store.settings.options)
            assertEquals(false, store.settings.options[SaoOption.FORCE_HUD.name])
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(dir) }
    }
}
