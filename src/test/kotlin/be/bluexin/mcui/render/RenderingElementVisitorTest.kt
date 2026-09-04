package be.bluexin.mcui.render

import be.bluexin.mcui.render.element.GroupElement
import be.bluexin.mcui.render.element.RectangleElement
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenderingElementVisitorTest {
    @Test
    fun `restores nested scissor and transforms when drawing fails`() {
        val operations = RecordingOperations(failOnFill = true)
        val tree = GroupElement(
            renderState = ResolvedRenderState(clip = ClipRect(0, 0, 20, 20)),
            transform = ResolvedTransform(x = 4f, y = 6f),
            children = listOf(
                RectangleElement(width = 10, height = 10, color = ArgbColor.WHITE),
            ),
        )

        assertFailsWith<ExpectedRenderFailure> {
            RenderingElementVisitor(operations).render(tree, RenderContext(0f, 320, 180))
        }

        assertEquals(0, operations.transformDepth)
        assertEquals(0, operations.scissorDepth)
        assertEquals(
            listOf("push", "translate", "scale", "scissor+", "push", "translate", "scale", "fill", "pop", "scissor-", "pop"),
            operations.events,
        )
    }

    private class ExpectedRenderFailure : RuntimeException()

    private class RecordingOperations(private val failOnFill: Boolean) : GuiRenderOperations {
        val events = mutableListOf<String>()
        var transformDepth = 0
        var scissorDepth = 0

        override fun pushTransform() {
            events += "push"
            transformDepth++
        }

        override fun popTransform() {
            events += "pop"
            transformDepth--
        }

        override fun translate(x: Float, y: Float, z: Float) {
            events += "translate"
        }

        override fun scale(x: Float, y: Float) {
            events += "scale"
        }

        override fun fill(x: Int, y: Int, width: Int, height: Int, color: ArgbColor) {
            events += "fill"
            if (failOnFill) throw ExpectedRenderFailure()
        }

        override fun texture(
            texture: ResourceLocation,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            u: Float,
            v: Float,
            sourceWidth: Int,
            sourceHeight: Int,
            textureWidth: Int,
            textureHeight: Int,
            tint: ArgbColor,
        ) = Unit

        override fun text(
            text: Component,
            x: Int,
            y: Int,
            color: ArgbColor,
            shadow: Boolean,
            centered: Boolean,
        ) = Unit

        override fun text(
            text: String,
            x: Int,
            y: Int,
            color: ArgbColor,
            shadow: Boolean,
            centered: Boolean,
        ) = Unit

        override fun item(stack: ItemStack, x: Int, y: Int, decorations: Boolean, countText: String?) = Unit

        override fun enableScissor(rect: ClipRect) {
            events += "scissor+"
            scissorDepth++
        }

        override fun disableScissor() {
            events += "scissor-"
            scissorDepth--
        }

        override fun close() = Unit
    }
}
