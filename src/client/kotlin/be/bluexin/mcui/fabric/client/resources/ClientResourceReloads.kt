package be.bluexin.mcui.fabric.client.resources

import be.bluexin.mcui.Constants
import be.bluexin.mcui.themes.MCUIThemes
import be.bluexin.mcui.themes.ThemeIssueSeverity
import be.bluexin.mcui.themes.ThemeResourceLoader
import be.bluexin.mcui.util.mcuiId
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Fabric-facing resource reload boundary for the phase-three theme pipeline. */
object ClientResourceReloads {
    private val registered = AtomicBoolean(false)
    private val reloadCounter = AtomicLong(0)
    private val themeLoader = ThemeResourceLoader()

    val revision: Long
        get() = reloadCounter.get()

    fun register() {
        if (!registered.compareAndSet(false, true)) return

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
            .registerReloadListener(object : SimpleSynchronousResourceReloadListener {
                override fun getFabricId(): ResourceLocation = mcuiId("foundation_reload")

                override fun onResourceManagerReload(resourceManager: ResourceManager) {
                    val currentRevision = reloadCounter.incrementAndGet()
                    val result = themeLoader.load(resourceManager)
                    result.issues.forEach { issue ->
                        val message = "MCUI theme {} [resource={}, theme={}, field={}]: {}"
                        val themeId = issue.themeId?.toString() ?: "unknown"
                        if (issue.severity == ThemeIssueSeverity.ERROR) {
                            Constants.LOG.error(message, "error", issue.resource, themeId, issue.field, issue.message)
                        } else {
                            Constants.LOG.warn(message, "warning", issue.resource, themeId, issue.field, issue.message)
                        }
                    }

                    val applied = MCUIThemes.manager.apply(currentRevision, result)
                    if (!applied) {
                        Constants.LOG.error(
                            "MCUI theme reload revision {} failed before snapshot creation; preserving revision {}: {}",
                            currentRevision,
                            MCUIThemes.manager.snapshot.revision,
                            result.fatalError,
                        )
                        return
                    }
                    val snapshot = MCUIThemes.manager.snapshot
                    Constants.LOG.info(
                        "MCUI theme reload revision {} complete: discovered={}, loaded={}, failed={}, active={}, source={}",
                        currentRevision,
                        result.discoveredCount,
                        result.themes.size,
                        result.failedCount,
                        snapshot.activeTheme.id,
                        snapshot.activeTheme.sourcePack,
                    )
                    result.themes.values.forEach { theme ->
                        Constants.LOG.debug(
                            "Compiled theme {} from {} ({}, {} elements)",
                            theme.id,
                            theme.sourceResource,
                            theme.sourcePack,
                            theme.elementCount,
                        )
                    }
                }
            })
    }
}
