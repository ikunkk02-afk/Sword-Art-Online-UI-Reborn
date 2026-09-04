package be.bluexin.mcui.util

import java.util.ArrayDeque
import java.util.LinkedHashMap

/**
 * A stack of maps where keys in the most recently pushed layer shadow keys in older layers.
 */
class LayeredMap<K, V> : Map<K, V> {
    private val layers = ArrayDeque<Map<K, V>>()

    private fun snapshot(): Map<K, V> = LinkedHashMap<K, V>().also { result ->
        layers.descendingIterator().forEachRemaining(result::putAll)
    }

    override val entries: Set<Map.Entry<K, V>>
        get() = snapshot().entries
    override val keys: Set<K>
        get() = snapshot().keys
    override val size: Int
        get() = snapshot().size
    override val values: Collection<V>
        get() = snapshot().values

    override fun containsKey(key: K): Boolean = layers.any { it.containsKey(key) }
    override fun containsValue(value: V): Boolean = snapshot().containsValue(value)

    override fun get(key: K): V? {
        for (layer in layers) {
            if (layer.containsKey(key)) return layer[key]
        }
        return null
    }

    override fun isEmpty(): Boolean = layers.all(Map<*, *>::isEmpty)

    operator fun plusAssign(layer: Map<K, V>) {
        layers.addFirst(layer)
    }

    fun pop(): Map<K, V> = layers.removeFirst()

    val canPop: Boolean
        get() = layers.isNotEmpty()
}
