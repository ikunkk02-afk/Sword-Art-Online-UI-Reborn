/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes

import be.bluexin.mcui.themes.legacy.LegacyJsonThemeLoader
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager

/** Minecraft-facing discovery and IO adapter. Parsing and compilation remain separate stages. */
class ThemeResourceLoader(
    private val parser: ThemeJsonParser = ThemeJsonParser(),
    private val legacyJsonLoader: LegacyJsonThemeLoader = LegacyJsonThemeLoader(),
) {
    fun load(resourceManager: ResourceManager): ThemeLoadResult = try {
        loadSafely(resourceManager)
    } catch (cause: Exception) {
        ThemeLoadResult(
            themes = emptyMap(),
            discoveredCount = 0,
            failedCount = 0,
            issues = emptyList(),
            fatalError = cause.message ?: cause::class.simpleName ?: "unknown loader failure",
        )
    }

    private fun loadSafely(resourceManager: ResourceManager): ThemeLoadResult {
        val metadataResources = resourceManager.listResources(THEMES_DIRECTORY) {
            it.path.endsWith("/$METADATA_FILE")
        }.toSortedMap(compareBy(ResourceLocation::toString))
        val metadataRoots = metadataResources.keys.mapTo(mutableSetOf()) {
            it.namespace to it.path.removeSuffix("/$METADATA_FILE")
        }
        val legacyResources = resourceManager.listResources(THEMES_DIRECTORY) {
            (it.path.endsWith("/hud.json") || it.path.endsWith("/hud.xml")) &&
                (it.namespace to it.path.substringBeforeLast('/')) !in metadataRoots
        }.toSortedMap(compareBy(ResourceLocation::toString))

        val issues = mutableListOf<ThemeIssue>()
        val definitions = mutableListOf<ThemeDefinition>()
        var failed = 0
        val candidateIds = metadataResources.keys.associateWith { location ->
            val rootPath = location.path.removeSuffix("/$METADATA_FILE")
            val themeName = rootPath.substringAfterLast('/')
            if (themeName == THEMES_DIRECTORY) null
            else runCatching { ThemeId(location.namespace, themeName) }.getOrNull()
        }
        val duplicateLocations = candidateIds.entries
            .filter { it.value != null }
            .groupBy({ it.value!! }, { it.key })
            .filterValues { it.size > 1 }

        metadataResources.forEach { (metadataLocation, metadataResource) ->
            val rootPath = metadataLocation.path.removeSuffix("/$METADATA_FILE")
            val themeName = rootPath.substringAfterLast('/')
            val themeId = candidateIds[metadataLocation]
            if (themeId == null) {
                issues += ThemeIssue(
                    ThemeIssueSeverity.ERROR,
                    metadataLocation.toString(),
                    themeId,
                    "resource.path",
                    "Theme metadata must be under themes/<theme-name>/$METADATA_FILE",
                )
                failed++
                return@forEach
            }
            duplicateLocations[themeId]?.let { locations ->
                issues += ThemeIssue(
                    ThemeIssueSeverity.ERROR,
                    metadataLocation.toString(),
                    themeId,
                    "id",
                    "Duplicate theme id '$themeId' is produced by ${locations.joinToString()}",
                )
                failed++
                return@forEach
            }

            val metadata = parseMetadata(metadataLocation, metadataResource, themeId, issues)
            if (metadata == null) {
                failed++
                return@forEach
            }

            val hudLocation = ResourceLocation.fromNamespaceAndPath(metadataLocation.namespace, "$rootPath/hud.json")
            val hudResource = resourceManager.getResource(hudLocation).orElse(null)
            if (hudResource == null) {
                val xmlLocation = ResourceLocation.fromNamespaceAndPath(metadataLocation.namespace, "$rootPath/hud.xml")
                val message = if (resourceManager.getResource(xmlLocation).isPresent) {
                    "Found hud.xml, but XML themes are deferred in phase three; a hud.json is required"
                } else {
                    "Missing required hud.json beside $METADATA_FILE"
                }
                issues += ThemeIssue(ThemeIssueSeverity.ERROR, hudLocation.toString(), themeId, "hud", message)
                failed++
                return@forEach
            }
            val document = parseDocument(hudLocation, hudResource, themeId, issues)
            if (document == null) {
                failed++
                return@forEach
            }
            definitions += ThemeDefinition(
                id = themeId,
                metadata = metadata,
                document = document,
                metadataResource = metadataLocation.toString(),
                hudResource = hudLocation.toString(),
                sourcePack = "metadata=${metadataResource.sourcePackId()}, hud=${hudResource.sourcePackId()}",
            )
        }

        legacyResources.forEach { (location, resource) ->
            val root = location.path.substringBeforeLast('/')
            val name = root.substringAfterLast('/')
            val id = if (name == THEMES_DIRECTORY) null else runCatching { ThemeId(location.namespace, name) }.getOrNull()
            if (id == null) {
                issues += ThemeIssue(
                    ThemeIssueSeverity.ERROR,
                    location.toString(),
                    null,
                    "resource.path",
                    "Legacy theme must be under themes/<theme-name>/hud.json",
                )
                failed++
                return@forEach
            }
            if (location.path.endsWith(".xml")) {
                issues += ThemeIssue(
                    ThemeIssueSeverity.ERROR,
                    location.toString(),
                    id,
                    "hud",
                    "External legacy XML support remains deferred; migrate the bundled theme to modern JSON",
                )
                failed++
                return@forEach
            }
            val text = readResource(location, resource, id, issues)
            if (text == null) {
                failed++
                return@forEach
            }
            val adaptation = legacyJsonLoader.parseHud(text, id, location.toString()).getOrElse { cause ->
                issues += ThemeIssue(
                    ThemeIssueSeverity.ERROR,
                    location.toString(),
                    id,
                    "hud",
                    "Malformed legacy HUD JSON: ${cause.message}",
                )
                failed++
                return@forEach
            }
            issues += adaptation.issues
            issues += ThemeIssue(
                ThemeIssueSeverity.WARNING,
                location.toString(),
                id,
                "metadata",
                "Loaded metadata-less legacy JSON through the resource-reload adapter",
            )
            definitions += ThemeDefinition(
                id = id,
                metadata = ThemeMetadata(
                    format = ThemeMetadata.LEGACY_ALPHA_FORMAT,
                    version = adaptation.document.version,
                    name = "Legacy theme: $name",
                    authors = listOf("Original MCUI theme authors"),
                ),
                document = adaptation.document,
                metadataResource = location.toString(),
                hudResource = location.toString(),
                sourcePack = resource.sourcePackId(),
            )
        }

        val compiler = ThemeCompiler { resourceManager.getResource(it).isPresent }
        val themes = linkedMapOf<ThemeId, ResolvedTheme>()
        definitions.sortedBy { it.id.toString() }.forEach { definition ->
            val result = compiler.compile(definition)
            issues += result.validation.issues
            if (result.theme == null) failed++ else themes[definition.id] = result.theme
        }

        return ThemeLoadResult(
            themes = themes.toMap(),
            discoveredCount = metadataResources.size + legacyResources.size,
            failedCount = failed,
            issues = issues.toList(),
        )
    }

    private fun parseMetadata(
        location: ResourceLocation,
        resource: Resource,
        themeId: ThemeId,
        issues: MutableList<ThemeIssue>,
    ): ThemeMetadata? {
        val text = readResource(location, resource, themeId, issues) ?: return null
        return parser.parseMetadata(text).getOrElse { cause ->
            issues += ThemeIssue(
                ThemeIssueSeverity.ERROR,
                location.toString(),
                themeId,
                "metadata",
                "Malformed metadata JSON: ${cause.message}",
            )
            null
        }
    }

    private fun parseDocument(
        location: ResourceLocation,
        resource: Resource,
        themeId: ThemeId,
        issues: MutableList<ThemeIssue>,
    ): ThemeDocument? {
        val text = readResource(location, resource, themeId, issues) ?: return null
        parser.parseDocument(text).getOrNull()?.let { return it }
        return legacyJsonLoader.parseHud(text, themeId, location.toString()).getOrElse { cause ->
            issues += ThemeIssue(
                ThemeIssueSeverity.ERROR,
                location.toString(),
                themeId,
                "hud",
                "Malformed modern or legacy HUD JSON: ${cause.message}",
            )
            return null
        }.also { issues += it.issues }.document
    }

    private fun readResource(
        location: ResourceLocation,
        resource: Resource,
        themeId: ThemeId,
        issues: MutableList<ThemeIssue>,
    ): String? = try {
        resource.openAsReader().use { it.readText() }
    } catch (cause: Exception) {
        issues += ThemeIssue(
            ThemeIssueSeverity.ERROR,
            location.toString(),
            themeId,
            "resource",
            "Could not read resource from pack '${resource.sourcePackId()}': ${cause.message}",
        )
        null
    }

    companion object {
        const val THEMES_DIRECTORY = "themes"
        const val METADATA_FILE = "theme.mcui.json"
    }
}
