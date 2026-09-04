package be.bluexin.mcui.render

import kotlin.test.Test
import kotlin.test.assertEquals

class ArgbColorTest {
    @Test
    fun `decodes canonical ARGB colors`() {
        assertChannels(ArgbColor.WHITE, 1f, 1f, 1f, 1f)
        assertChannels(ArgbColor(0x80FFFFFF.toInt()), 128f / 255f, 1f, 1f, 1f)
        assertChannels(ArgbColor(0xFFFF0000.toInt()), 1f, 1f, 0f, 0f)
        assertChannels(ArgbColor.TRANSPARENT, 0f, 0f, 0f, 0f)
    }

    private fun assertChannels(color: ArgbColor, alpha: Float, red: Float, green: Float, blue: Float) {
        assertEquals(alpha, color.alpha)
        assertEquals(red, color.red)
        assertEquals(green, color.green)
        assertEquals(blue, color.blue)
    }
}
