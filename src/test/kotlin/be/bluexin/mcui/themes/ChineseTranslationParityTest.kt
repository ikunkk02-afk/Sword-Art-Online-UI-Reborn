package be.bluexin.mcui.themes

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChineseTranslationParityTest {
    private val placeholder = Regex("%(?:\\d+\\$)?[a-zA-Z]")

    @Test fun `Chinese locale covers every English SAOUI key and preserves format placeholders`() {
        val loader = javaClass.classLoader
        fun locale(name: String) = Json.parseToJsonElement(
            requireNotNull(loader.getResourceAsStream("assets/saoui/lang/$name.json")).bufferedReader().use { it.readText() },
        ).jsonObject
        val english = locale("en_us")
        val chinese = locale("zh_cn")
        val translatableKeys = english.keys - "_comment"
        assertTrue(translatableKeys.all(chinese::containsKey), "Missing zh_cn keys: ${translatableKeys - chinese.keys}")
        english.keys.filterNot { it == "_comment" }.forEach { key ->
            assertEquals(
                placeholder.findAll(english.getValue(key).jsonPrimitive.content).map { it.value }.toList(),
                placeholder.findAll(chinese.getValue(key).jsonPrimitive.content).map { it.value }.toList(),
                "Formatting placeholders differ for $key",
            )
        }
    }
}
