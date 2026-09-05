package be.bluexin.mcui.themes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThemeJsonParserTest {
    private val parser = ThemeJsonParser()

    @Test
    fun `metadata parses original fields and defaults extensions`() {
        val metadata = parser.parseMetadata(
            """{"format":"mcui:alpha","version":"0.1-alpha"}""",
        ).getOrThrow()

        assertEquals("mcui:alpha", metadata.format)
        assertEquals("0.1-alpha", metadata.version)
        assertEquals("fragments", metadata.fragments)
        assertEquals("widgets", metadata.widgets)
        assertEquals("scripts", metadata.scripts)
        assertTrue(metadata.authors.isEmpty())
    }

    @Test
    fun `metadata supports phase three descriptive fields`() {
        val metadata = parser.parseMetadata(
            """
                {
                  "format": "mcui:resolved-v1",
                  "id": "mcui:test",
                  "name": "Test Theme",
                  "version": "1.2.3",
                  "authors": ["Alice", "Bob"],
                  "description": "Description",
                  "website": "https://example.invalid",
                  "supportedVersion": "1.21.1",
                  "extends": "mcui:base"
                }
            """.trimIndent(),
        ).getOrThrow()

        assertEquals("mcui:test", metadata.id)
        assertEquals("Test Theme", metadata.name)
        assertEquals(listOf("Alice", "Bob"), metadata.authors)
        assertEquals("mcui:base", metadata.extendsTheme)
    }

    @Test
    fun `missing required metadata format fails`() {
        assertTrue(parser.parseMetadata("""{"version":"1"}""").isFailure)
    }

    @Test
    fun `malformed metadata JSON fails`() {
        assertTrue(parser.parseMetadata("""{"format":"mcui:resolved-v1","name":}""").isFailure)
    }

    @Test
    fun `valid minimal theme and element defaults parse`() {
        val document = parser.parseDocument(
            """{"root":{"type":"text","text":"hello"}}""",
        ).getOrThrow()

        assertEquals("1", document.version)
        val root = assertNotNull(document.root)
        assertEquals("text", root.type)
        assertTrue(root.enabled)
        assertFalse(root.shadow)
        assertFalse(root.centered)
        assertEquals(TransformDefinition(), root.transform)
    }

    @Test
    fun `group children parse recursively`() {
        val document = parser.parseDocument(
            """
                {"root":{"type":"group","children":[
                  {"type":"rectangle","width":10,"height":5,"color":"#80FF0000"},
                  {"type":"text","text":"child"}
                ]}}
            """.trimIndent(),
        ).getOrThrow()

        val root = assertNotNull(document.root)
        assertEquals(2, root.children.size)
        assertEquals(0x80FF0000.toInt(), root.children.first().color?.value)
    }

    @Test
    fun `ARGB accepts RGB ARGB and unsigned numeric forms`() {
        fun color(json: String) = parser.parseDocument(
            """{"root":{"type":"rectangle","width":1,"height":1,"color":$json}}""",
        ).getOrThrow().let { assertNotNull(it.root).color }

        assertEquals(0xFFFF0000.toInt(), color("\"#FF0000\"")?.value)
        assertEquals(0x80FF0000.toInt(), color("\"0x80FF0000\"")?.value)
        assertEquals(0xFFFFFFFF.toInt(), color("4294967295")?.value)
    }
}
