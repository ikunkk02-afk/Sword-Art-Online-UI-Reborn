package be.bluexin.mcui.themes

import be.bluexin.mcui.render.element.GroupElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ThemeManagerTest {
    private val preferred = ThemeId("mcui", "preferred")

    @Test
    fun `valid snapshot replacement selects preferred theme`() {
        val manager = ThemeManager(preferred)
        val theme = resolvedTheme(preferred)

        assertTrue(manager.apply(1, validResult(theme)))
        assertEquals(1, manager.snapshot.revision)
        assertSame(theme, manager.activeTheme)
    }

    @Test
    fun `missing active theme uses empty fallback`() {
        val manager = ThemeManager(preferred)

        assertTrue(manager.apply(2, validResult(resolvedTheme(ThemeId("mcui", "other")))))
        assertSame(ThemeSnapshot.FALLBACK_THEME, manager.activeTheme)
    }

    @Test
    fun `fatal load preserves previous good snapshot`() {
        val manager = ThemeManager(preferred)
        val theme = resolvedTheme(preferred)
        manager.apply(3, validResult(theme))
        val before = manager.snapshot

        val applied = manager.apply(
            4,
            ThemeLoadResult(emptyMap(), 0, 0, emptyList(), fatalError = "resource manager failed"),
        )

        assertFalse(applied)
        assertSame(before, manager.snapshot)
        assertSame(theme, manager.activeTheme)
    }

    @Test
    fun `user selection survives reload and missing selection preserves active snapshot`() {
        val manager = ThemeManager(preferred)
        val first = resolvedTheme(preferred)
        val other = resolvedTheme(ThemeId("test", "other"))
        val result = validResult(first).copy(themes = mapOf(first.id to first, other.id to other))
        manager.apply(1, result)
        assertTrue(manager.select(other.id))
        assertSame(other, manager.activeTheme)
        assertFalse(manager.select(ThemeId("test", "missing")))
        assertSame(other, manager.activeTheme)
        manager.apply(2, result)
        assertSame(other, manager.activeTheme)
    }

    private fun validResult(theme: ResolvedTheme) = ThemeLoadResult(
        themes = mapOf(theme.id to theme),
        discoveredCount = 1,
        failedCount = 0,
        issues = emptyList(),
    )

    private fun resolvedTheme(id: ThemeId) = ResolvedTheme(
        id = id,
        metadata = ThemeMetadata(ThemeMetadata.RESOLVED_V1_FORMAT, name = "Test"),
        hudRoot = GroupElement(children = emptyList()),
        sourcePack = "test",
        sourceResource = "test:hud.json",
        elementCount = 1,
    )
}
