package be.bluexin.mcui.themes

import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.RectangleElement
import be.bluexin.mcui.render.element.TextElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeCompilerTest {
    private val parser = ThemeJsonParser()

    @Test
    fun `definition compiles to resolved hierarchy and transforms`() {
        val result = compile(
            """
                {"root":{"type":"group","transform":{"x":12,"y":8,"z":2,"scale":0.5},"children":[
                  {"type":"rectangle","width":30,"height":10,"color":"#8044AA66"},
                  {"type":"text","text":"resolved","shadow":true}
                ]}}
            """.trimIndent(),
        )

        assertTrue(result.validation.isValid)
        val theme = assertNotNull(result.theme)
        assertEquals(3, theme.elementCount)
        val root = assertIs<GroupElement>(theme.hudRoot)
        assertEquals(12f, root.transform.x)
        assertEquals(0.5f, root.transform.scaleX)
        assertEquals(0x8044AA66.toInt(), assertIs<RectangleElement>(root.children[0]).color.value)
        val text = assertIs<TextElement>(root.children[1])
        assertTrue(text.shadow)
        assertEquals(0xFFFFFFFF.toInt(), text.color.value)
    }

    @Test
    fun `unknown element type invalidates entire theme with path`() {
        val result = compile("""{"root":{"type":"something_unknown"}}""")

        assertNull(result.theme)
        assertFalse(result.validation.isValid)
        assertTrue(result.validation.issues.single().field.contains("root.type"))
        assertTrue(result.validation.issues.single().message.contains("something_unknown"))
    }

    @Test
    fun `invalid texture ResourceLocation invalidates theme`() {
        val result = compile(
            """{"root":{"type":"texture","texture":"Bad Namespace:path","width":16,"height":16}}""",
        )

        assertNull(result.theme)
        assertTrue(result.validation.issues.any { it.field == "root.texture" && it.message.contains("Invalid") })
    }

    @Test
    fun `missing type-specific field invalidates theme`() {
        val result = compile("""{"root":{"type":"rectangle","height":4,"color":"#FFFFFFFF"}}""")

        assertNull(result.theme)
        assertTrue(result.validation.issues.any { it.field == "root.width" })
    }

    @Test
    fun `resolved metadata requires name and author`() {
        val definition = ThemeDefinition(
            id = ThemeId("mcui", "test"),
            metadata = ThemeMetadata(format = ThemeMetadata.RESOLVED_V1_FORMAT),
            document = parser.parseDocument("""{"root":{"type":"group"}}""").getOrThrow(),
            metadataResource = "mcui:themes/test/theme.mcui.json",
            hudResource = "mcui:themes/test/hud.json",
            sourcePack = "test",
        )

        val result = ThemeCompiler(textureExists = { true }).compile(definition)

        assertNull(result.theme)
        assertTrue(result.validation.issues.any { it.field == "metadata.name" })
        assertTrue(result.validation.issues.any { it.field == "metadata.version" })
        assertTrue(result.validation.issues.any { it.field == "metadata.authors" })
        assertTrue(result.validation.issues.filter { it.field.startsWith("metadata") }
            .all { it.resource.endsWith("theme.mcui.json") })
    }

    @Test
    fun `missing texture is a warning and compiles once`() {
        val result = compile(
            """{"root":{"type":"texture","texture":"mcui:missing.png","width":16,"height":16}}""",
        )

        assertNotNull(result.theme)
        assertTrue(result.validation.isValid)
        assertTrue(result.validation.issues.any { it.severity == ThemeIssueSeverity.WARNING })
    }

    private fun compile(documentJson: String): ThemeCompileResult {
        val definition = ThemeDefinition(
            id = ThemeId("mcui", "test"),
            metadata = ThemeMetadata(
                format = ThemeMetadata.RESOLVED_V1_FORMAT,
                version = "test",
                id = "mcui:test",
                name = "Test",
                authors = listOf("Tester"),
            ),
            document = parser.parseDocument(documentJson).getOrThrow(),
            metadataResource = "mcui:themes/test/theme.mcui.json",
            hudResource = "mcui:themes/test/hud.json",
            sourcePack = "test",
        )
        return ThemeCompiler(textureExists = { false }).compile(definition)
    }
}
