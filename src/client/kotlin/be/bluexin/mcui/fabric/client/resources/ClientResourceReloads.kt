package be.bluexin.mcui.fabric.client.resources

import be.bluexin.mcui.Constants
import be.bluexin.mcui.util.mcuiId
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Stable Fabric-facing boundary for the theme resource loader planned for phase two.
 */
object ClientResourceReloads {
    private val registered = AtomicBoolean(false)
    private val reloadCounter = AtomicLong(0)

    val revision: Long
        get() = reloadCounter.get()

    fun register() {
        if (!registered.compareAndSet(false, true)) return

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
            .registerReloadListener(object : SimpleSynchronousResourceReloadListener {
                override fun getFabricId(): ResourceLocation = mcuiId("foundation_reload")

                override fun onResourceManagerReload(resourceManager: ResourceManager) {
                    val currentRevision = reloadCounter.incrementAndGet()
                    if (currentRevision == 1L) {
                        Constants.LOG.info("MCUI client resources ready (reload revision {})", currentRevision)
                    } else {
                        Constants.LOG.debug("MCUI client resources reloaded (revision {})", currentRevision)
                    }
                }
            })
    }
}
