/*
 * Copyright (C) 2016-2024 Arnaud 'Bluexin' Solé
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package be.bluexin.mcui.themes.legacy

import be.bluexin.mcui.themes.ArgbColorDefinition
import be.bluexin.mcui.themes.AnimationDefinition
import be.bluexin.mcui.themes.AnimationEasing
import be.bluexin.mcui.themes.AnimationProperty
import be.bluexin.mcui.themes.AnimationTrigger
import be.bluexin.mcui.themes.ElementDefinition
import be.bluexin.mcui.themes.FragmentArguments
import be.bluexin.mcui.themes.HotbarOrientation
import be.bluexin.mcui.themes.HudAnchor
import be.bluexin.mcui.themes.HudItemSource
import be.bluexin.mcui.themes.HudTextSource
import be.bluexin.mcui.themes.HudValueSource
import be.bluexin.mcui.themes.ProgressDirection
import be.bluexin.mcui.themes.TextureRegionDefinition
import be.bluexin.mcui.themes.ThemeId
import be.bluexin.mcui.themes.ThemeIssue
import be.bluexin.mcui.themes.ThemeIssueSeverity
import be.bluexin.mcui.themes.TransformDefinition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Converts the high-value legacy element subset. No legacy element survives into rendering and
 * no expression engine is invoked; recognized HUD bindings become typed modern sources.
 */
class LegacyElementAdapter(
    private val themeId: ThemeId,
    private val resource: String,
    private val issues: MutableList<ThemeIssue>,
) {
    fun adaptGroup(body: JsonObject, path: String, inheritedTexture: String?): ElementDefinition? {
        val texture = stringValue(body["texture"]) ?: inheritedTexture
        val children = (body["children"] as? JsonArray).orEmpty().mapIndexedNotNull { index, child ->
            adaptElement(child, "$path.children[$index]", texture)
        }
        if (children.isEmpty()) return null
        return ElementDefinition(
            type = "group",
            name = stringValue(body["name"]),
            enabled = enabled(body["enabled"], "$path.enabled"),
            transform = transform(body, path),
            children = children,
            animations = animations(body, path),
        )
    }

    fun adaptElement(node: JsonElement, path: String, inheritedTexture: String?): ElementDefinition? {
        val wrapper = node as? JsonObject ?: run {
            warning(path, "Legacy element must be an object")
            return null
        }
        if (wrapper.size != 1) {
            warning(path, "Legacy key-discriminator element must contain exactly one entry")
            return null
        }
        val (discriminator, rawBody) = wrapper.entries.first()
        val body = rawBody as? JsonObject ?: run {
            warning(path, "Legacy element '$discriminator' body must be an object")
            return null
        }
        val type = discriminator.substringBefore(':').lowercase()
        val encodedName = discriminator.substringAfter(':', "").ifBlank { null }
        val elementPath = "$path.${encodedName ?: discriminator.substringBefore(':')}"
        return when (type) {
            "elementgroup", "group", "fragment" -> adaptGroup(body.withName(encodedName), elementPath, inheritedTexture)
            "widget" -> adaptWidget(body.withName(encodedName), elementPath, inheritedTexture)
            "glrectangle", "rectangle", "texture", "image" ->
                adaptRectangle(body.withName(encodedName), elementPath, inheritedTexture)
            "glstring", "text" -> adaptText(body.withName(encodedName), elementPath)
            "fragmentreference", "fragment_reference" -> adaptFragmentReference(body.withName(encodedName), elementPath)
            "glhotbaritem", "hotbaritem" -> adaptHotbarItem(body.withName(encodedName), elementPath, inheritedTexture)
            "repetitiongroup" -> adaptHotbarRepetition(body.withName(encodedName), elementPath, inheritedTexture)
            "hud" -> adaptGroup(body.withName(encodedName), elementPath, inheritedTexture)
            "rawelement" -> {
                warning(elementPath, "RawElement is deferred because it requires dynamic expression execution")
                null
            }
            else -> {
                warning(elementPath, "Unsupported legacy element '$discriminator'")
                null
            }
        }
    }

    private fun adaptRectangle(body: JsonObject, path: String, inheritedTexture: String?): ElementDefinition? {
        if (body["clipping"] != null || body["clip"] != null) {
            warning("$path.clipping", "Legacy arbitrary rectangle clipping is deferred; progress bindings use modern clipping")
        }
        val widthBinding = progressBinding(body["w"] ?: body["srcW"])
        val width = widthBinding?.first ?: dimension(body["w"], "$path.w")
        val height = dimension(body["h"], "$path.h")
        if (width == null || height == null || width <= 0 || height <= 0) {
            warning(path, "Legacy rectangle has invalid or dynamic dimensions")
            return null
        }
        val texture = stringValue(body["texture"]) ?: inheritedTexture
        val color = color(body["rgba"], "$path.rgba")
        if (widthBinding != null && texture != null) {
            return ElementDefinition(
                type = "textured_progress_bar",
                name = stringValue(body["name"]),
                enabled = enabled(body["enabled"], "$path.enabled"),
                transform = transform(body, path),
                animations = animations(body, path),
                width = width,
                height = height,
                direction = ProgressDirection.LEFT_TO_RIGHT,
                valueSource = widthBinding.second,
                foregroundTexture = textureRegion(body, texture, width, height, color ?: ArgbColorDefinition.WHITE),
                clip = true,
            )
        }
        if (texture != null) {
            return ElementDefinition(
                type = "texture",
                name = stringValue(body["name"]),
                enabled = enabled(body["enabled"], "$path.enabled"),
                transform = transform(body, path),
                animations = animations(body, path),
                width = width,
                height = height,
                texture = texture,
                u = staticDouble(body["srcX"], "$path.srcX", 0.0),
                v = staticDouble(body["srcY"], "$path.srcY", 0.0),
                sourceWidth = dimension(body["srcW"], "$path.srcW") ?: width,
                sourceHeight = dimension(body["srcH"], "$path.srcH") ?: height,
                textureWidth = dimension(body["textureWidth"], "$path.textureWidth") ?: 256,
                textureHeight = dimension(body["textureHeight"], "$path.textureHeight") ?: 256,
                tint = color ?: ArgbColorDefinition.WHITE,
            )
        }
        if (color != null) {
            return ElementDefinition(
                type = "rectangle",
                name = stringValue(body["name"]),
                enabled = enabled(body["enabled"], "$path.enabled"),
                transform = transform(body, path),
                animations = animations(body, path),
                width = width,
                height = height,
                color = color,
            )
        }
        warning(path, "Legacy rectangle has neither a texture nor a static color")
        return null
    }

    private fun adaptText(body: JsonObject, path: String): ElementDefinition? {
        val expression = expression(body["text"])
        val dynamicValue = expression?.let(::textValueSource)
        val dynamicText = expression?.let(::textSource)
        val literal = if (dynamicValue == null && dynamicText == null) staticText(expression) else null
        if (dynamicValue == null && dynamicText == null && literal == null) {
            warning("$path.text", "Deferred dynamic legacy text expression '${expression.orEmpty().take(120)}'")
            return null
        }
        val legacyHeight = staticDouble(body["h"], "$path.h", 0.0)
        val base = transform(body, path)
        return ElementDefinition(
            type = "text",
            name = stringValue(body["name"]),
            enabled = enabled(body["enabled"], "$path.enabled"),
            transform = base.copy(y = base.y + legacyHeight / 2.0),
            animations = animations(body, path),
            text = literal,
            valueSource = dynamicValue,
            textSource = dynamicText,
            color = color(body["rgba"], "$path.rgba") ?: ArgbColorDefinition.WHITE,
            shadow = staticBoolean(body["shadow"], "$path.shadow", true),
            centered = staticBoolean(body["centered"], "$path.centered", true),
        )
    }

    private fun adaptFragmentReference(body: JsonObject, path: String): ElementDefinition? {
        val id = stringValue(body["id"])
        if (id.isNullOrBlank()) {
            warning("$path.id", "Legacy FragmentReference is missing id")
            return null
        }
        val arguments = fragmentArguments(body["arguments"] ?: body["variables"], "$path.variables")
        return ElementDefinition(
            type = "fragment_reference",
            name = stringValue(body["name"]),
            enabled = enabled(body["enabled"], "$path.enabled"),
            transform = transform(body, path),
            fragment = id,
            fragmentArguments = arguments,
            animations = animations(body, path),
        )
    }

    private fun adaptHotbarItem(body: JsonObject, path: String, inheritedTexture: String?): ElementDefinition? {
        if (body["hand"] != null) {
            warning(path, "Hand-dependent legacy GLHotbarItem placement is deferred")
            return null
        }
        val slot = staticInt(body["slot"], "$path.slot", null)
        if (slot == null || slot !in 0..8) {
            warning(path, "GLHotbarItem requires a static slot from 0 to 8 outside a supported repetition group")
            return null
        }
        val background = adaptRectangle(body, "$path.background", inheritedTexture)
        val item = ElementDefinition(
            type = "dynamic_item",
            transform = TransformDefinition(
                x = staticDouble(body["itemXoffset"], "$path.itemXoffset", 2.0),
                y = staticDouble(body["itemYoffset"], "$path.itemYoffset", 2.0),
                z = 1.0,
            ),
            itemSource = HudItemSource.entries[slot],
            decorations = true,
        )
        return ElementDefinition(
            type = "group",
            name = stringValue(body["name"]),
            enabled = enabled(body["enabled"], "$path.enabled"),
            transform = transform(body, path),
            children = listOfNotNull(background?.copy(transform = TransformDefinition()), item),
            animations = animations(body, path),
        )
    }

    private fun adaptHotbarRepetition(
        body: JsonObject,
        path: String,
        inheritedTexture: String?,
    ): ElementDefinition? {
        val amount = staticInt(body["amount"], "$path.amount", null)
        val child = (body["children"] as? JsonArray)?.singleOrNull() as? JsonObject
        val entry = child?.entries?.singleOrNull()
        if (amount != 9 || entry == null || entry.key.substringBefore(':').lowercase() !in HOTBAR_ITEM_TYPES) {
            warning(path, "RepetitionGroup is deferred unless it is the standard nine-slot GLHotbarItem pattern")
            return null
        }
        val item = entry.value as? JsonObject ?: return null
        val xLine = linearIndexExpression(expression(item["x"]))
        val yLine = linearIndexExpression(expression(item["y"]))
        val orientation = if (yLine != null && (xLine == null || abs(yLine.second) >= abs(xLine.second))) {
            HotbarOrientation.VERTICAL
        } else {
            HotbarOrientation.HORIZONTAL
        }
        val line = if (orientation == HotbarOrientation.VERTICAL) yLine else xLine
        val slotSize = dimension(item["w"], "$path.slotSize") ?: 20
        val spacing = ((line?.second ?: slotSize.toDouble()).roundToInt() - slotSize).coerceAtLeast(0)
        val inherited = stringValue(body["texture"]) ?: inheritedTexture
        val texture = stringValue(item["texture"]) ?: inherited
        val colors = selectedColors(expression(item["rgba"]))
        val base = transform(body, path)
        val start = line?.first ?: 0.0
        val adjusted = if (orientation == HotbarOrientation.VERTICAL) base.copy(y = base.y + start)
        else base.copy(x = base.x + start)
        val region = texture?.let {
            textureRegion(item, it, slotSize, dimension(item["h"], "$path.slotHeight") ?: slotSize, colors?.second ?: ArgbColorDefinition.WHITE)
        }
        val selectedRegion = texture?.let {
            textureRegion(item, it, slotSize, dimension(item["h"], "$path.slotHeight") ?: slotSize, colors?.first ?: ArgbColorDefinition.WHITE)
        }
        return ElementDefinition(
            type = "hotbar",
            name = stringValue(body["name"]),
            enabled = enabled(body["enabled"], "$path.enabled"),
            transform = adjusted,
            animations = animations(body, path),
            slotSize = slotSize,
            slotSpacing = spacing,
            itemXOffset = staticInt(item["itemXoffset"], "$path.itemXoffset", 2) ?: 2,
            itemYOffset = staticInt(item["itemYoffset"], "$path.itemYoffset", 2) ?: 2,
            slotTexture = region,
            selectedSlotTexture = selectedRegion,
            orientation = orientation,
            decorations = true,
        )
    }

    private fun adaptWidget(body: JsonObject, path: String, inheritedTexture: String?): ElementDefinition? {
        listOf("onClick", "onMouseOver", "onLoseFocus", "tooltip", "active").forEach { field ->
            if (body[field] != null) warning("$path.$field", "Legacy Widget interaction '$field' is deferred to the screen phase")
        }
        return adaptGroup(body, path, inheritedTexture)
    }

    private fun fragmentArguments(node: JsonElement?, path: String): FragmentArguments {
        val body = node as? JsonObject ?: run {
            if (node != null && node !is JsonNull) warning(path, "Unsupported legacy fragment variables; only literal object overrides are accepted")
            return FragmentArguments()
        }
        val recognized = setOf("enabled", "x", "y", "z", "scale", "color", "text", "texture", "tint")
        body.keys.filterNot(recognized::contains).forEach { key ->
            warning("$path.$key", "Unsupported fragment argument '$key'; literal root-field overrides only")
        }
        return FragmentArguments(
            enabled = expression(body["enabled"])?.toBooleanStrictOrNull(),
            x = staticDoubleOrNull(body["x"]),
            y = staticDoubleOrNull(body["y"]),
            z = staticDoubleOrNull(body["z"]),
            scale = staticDoubleOrNull(body["scale"]),
            color = color(body["color"], "$path.color"),
            text = stringValue(body["text"]),
            texture = stringValue(body["texture"]),
            tint = color(body["tint"], "$path.tint"),
        )
    }

    private fun animations(body: JsonObject, path: String): List<AnimationDefinition> {
        val node = body["animations"] ?: body["animation"] ?: return emptyList()
        val entries = when (node) {
            is JsonArray -> node
            is JsonObject -> JsonArray(listOf(node))
            else -> {
                warning("$path.animations", "Legacy animator must be an object or array; animator deferred")
                return emptyList()
            }
        }
        return entries.mapIndexedNotNull { index, entry ->
            val animation = entry as? JsonObject ?: run {
                warning("$path.animations[$index]", "Legacy animator entry must be an object")
                return@mapIndexedNotNull null
            }
            val field = "$path.animations[$index]"
            val propertyName = stringValue(animation["property"] ?: animation["target"])?.lowercase()?.replace("-", "_")
            val property = when (propertyName) {
                "alpha", "opacity" -> AnimationProperty.ALPHA
                "x", "translation_x", "translate_x" -> AnimationProperty.TRANSLATION_X
                "y", "translation_y", "translate_y" -> AnimationProperty.TRANSLATION_Y
                "scale" -> AnimationProperty.SCALE
                "progress", "value" -> AnimationProperty.PROGRESS
                "color", "rgba", "argb" -> AnimationProperty.COLOR
                else -> {
                    warning("$field.property", "Unsupported legacy animator property '$propertyName'; animator deferred")
                    return@mapIndexedNotNull null
                }
            }
            val easingName = stringValue(animation["easing"])?.uppercase()?.replace('-', '_') ?: "LINEAR"
            val easing = when (easingName.replace("EASEIN", "EASE_IN").replace("EASEOUT", "EASE_OUT")) {
                "LINEAR" -> AnimationEasing.LINEAR
                "EASE_IN", "EASE_IN_QUAD", "QUAD_IN" -> AnimationEasing.QUAD_IN
                "EASE_OUT", "EASE_OUT_QUAD", "QUAD_OUT" -> AnimationEasing.QUAD_OUT
                "EASE_IN_OUT", "EASE_IN_OUT_QUAD", "QUAD_IN_OUT" -> AnimationEasing.QUAD_IN_OUT
                "EASE_IN_CUBIC", "CUBIC_IN" -> AnimationEasing.CUBIC_IN
                "EASE_OUT_CUBIC", "CUBIC_OUT" -> AnimationEasing.CUBIC_OUT
                "EASE_IN_OUT_CUBIC", "CUBIC_IN_OUT" -> AnimationEasing.CUBIC_IN_OUT
                else -> {
                    warning("$field.easing", "Unsupported legacy easing '$easingName'; using LINEAR")
                    AnimationEasing.LINEAR
                }
            }
            val trigger = when (stringValue(animation["trigger"])?.uppercase()) {
                null, "ON_SHOW", "SHOW" -> AnimationTrigger.ON_SHOW
                "ON_HIDE", "HIDE" -> AnimationTrigger.ON_HIDE
                "ON_VALUE_CHANGE", "VALUE_CHANGE", "CHANGE" -> AnimationTrigger.ON_VALUE_CHANGE
                else -> {
                    warning("$field.trigger", "Unsupported legacy animator trigger; animator deferred")
                    return@mapIndexedNotNull null
                }
            }
            val from = animationValue(animation["from"], property, "$field.from")
            val to = animationValue(animation["to"], property, "$field.to")
            AnimationDefinition(
                property = property,
                duration = staticInt(animation["duration"], "$field.duration", 200) ?: 200,
                delay = staticInt(animation["delay"] ?: animation["start"], "$field.delay", 0) ?: 0,
                easing = easing,
                from = from,
                to = to,
                trigger = trigger,
            )
        }
    }

    private fun animationValue(node: JsonElement?, property: AnimationProperty, path: String): Double? {
        if (node == null || node is JsonNull) return null
        if (property == AnimationProperty.COLOR) {
            return color(node, path)?.value?.toUInt()?.toLong()?.toDouble()
        }
        return staticDoubleOrNull(node) ?: run {
            warning(path, "Legacy animator endpoint must be a literal number")
            null
        }
    }

    private fun textureRegion(
        body: JsonObject,
        texture: String,
        width: Int,
        height: Int,
        tint: ArgbColorDefinition,
    ) = TextureRegionDefinition(
        texture = texture,
        u = staticDouble(body["srcX"], "texture.srcX", 0.0),
        v = staticDouble(body["srcY"], "texture.srcY", 0.0),
        sourceWidth = dimension(body["srcW"], "texture.srcW") ?: width,
        sourceHeight = dimension(body["srcH"], "texture.srcH") ?: height,
        textureWidth = dimension(body["textureWidth"], "texture.textureWidth") ?: 256,
        textureHeight = dimension(body["textureHeight"], "texture.textureHeight") ?: 256,
        tint = tint,
    )

    private fun transform(body: JsonObject, path: String): TransformDefinition {
        val xAxis = axis(body["x"], "$path.x", "scaledwidth")
        val yAxis = axis(body["y"], "$path.y", "scaledheight")
        val anchor = when (yAxis.alignment) {
            Alignment.START -> when (xAxis.alignment) {
                Alignment.START -> HudAnchor.TOP_LEFT
                Alignment.CENTER -> HudAnchor.TOP_CENTER
                Alignment.END -> HudAnchor.TOP_RIGHT
            }
            Alignment.CENTER -> when (xAxis.alignment) {
                Alignment.START -> HudAnchor.CENTER_LEFT
                Alignment.CENTER -> HudAnchor.CENTER
                Alignment.END -> HudAnchor.CENTER_RIGHT
            }
            Alignment.END -> when (xAxis.alignment) {
                Alignment.START -> HudAnchor.BOTTOM_LEFT
                Alignment.CENTER -> HudAnchor.BOTTOM_CENTER
                Alignment.END -> HudAnchor.BOTTOM_RIGHT
            }
        }
        return TransformDefinition(
            x = xAxis.offset,
            y = yAxis.offset,
            z = staticDouble(body["z"], "$path.z", 0.0),
            scale = staticDouble(body["scale"], "$path.scale", 1.0),
            anchor = anchor,
        )
    }

    private fun axis(node: JsonElement?, path: String, dimension: String): Axis {
        if (node == null || node is JsonNull) return Axis(Alignment.START, 0.0)
        val raw = expression(node)?.lowercase()?.replace(" ", "") ?: return Axis(Alignment.START, 0.0)
        raw.toDoubleOrNull()?.let { return Axis(Alignment.START, it) }
        Regex("^$dimension/2(?:\\.0)?([+-].+)?$").matchEntire(raw)?.let {
            return Axis(Alignment.CENTER, it.groupValues[1].ifBlank { "0" }.toDoubleOrNull() ?: 0.0)
        }
        Regex("^$dimension([+-].+)?$").matchEntire(raw)?.let {
            return Axis(Alignment.END, it.groupValues[1].ifBlank { "0" }.toDoubleOrNull() ?: 0.0)
        }
        warning(path, "Deferred dynamic legacy position '$raw'; using zero offset")
        return Axis(Alignment.START, 0.0)
    }

    private fun enabled(node: JsonElement?, path: String): Boolean {
        val raw = expression(node)?.trim() ?: return true
        raw.toBooleanStrictOrNull()?.let { return it }
        val normalized = raw.lowercase().replace(" ", "")
        if ("vertical_hotbar" in normalized || "ver_hotbar" in normalized) return !normalized.startsWith("!")
        if ("hor_hotbar" in normalized) return normalized.startsWith("!")
        warning(path, "Deferred dynamic legacy enabled expression '$raw'; using enabled=true")
        return true
    }

    private fun progressBinding(node: JsonElement?): Pair<Int, HudValueSource>? {
        val raw = expression(node)?.replace(" ", "") ?: return null
        val match = PROGRESS_PATTERN.matchEntire(raw) ?: return null
        val width = match.groupValues[1].toDoubleOrNull()?.roundToInt() ?: return null
        val source = when (match.groupValues[2].lowercase()) {
            "healthpercent", "hppct" -> HudValueSource.PLAYER_HEALTH
            "foodpercent", "foodpct" -> HudValueSource.FOOD
            "airpercent", "airpct" -> HudValueSource.AIR
            "experience" -> HudValueSource.EXPERIENCE_PROGRESS
            "horsejump" -> HudValueSource.JUMP_PROGRESS
            "targethppct" -> HudValueSource.TARGET_HEALTH
            else -> return null
        }
        return width to source
    }

    private fun textValueSource(raw: String): HudValueSource? {
        val normalized = raw.lowercase().replace(" ", "")
        return when {
            normalized in setOf("hp", "health", "player.health") -> HudValueSource.PLAYER_HEALTH
            normalized in setOf("maxhp", "maxhealth", "player.maxhealth") -> HudValueSource.PLAYER_MAX_HEALTH
            normalized in setOf("food", "player.food") -> HudValueSource.FOOD
            normalized in setOf("air", "player.air") -> HudValueSource.AIR
            normalized in setOf("armor", "player.armor") -> HudValueSource.ARMOR
            normalized == "level" || normalized == "player.level" ||
                (normalized.startsWith("format(") && "level" in normalized) -> HudValueSource.EXPERIENCE_LEVEL
            normalized in setOf("targethp", "target.health") -> HudValueSource.TARGET_HEALTH
            else -> null
        }
    }

    private fun textSource(raw: String): HudTextSource? {
        val normalized = raw.lowercase().replace(" ", "")
        return when {
            normalized in setOf("username", "player.displayname", "player.name") -> HudTextSource.PLAYER_NAME
            normalized in setOf("targetname", "target.name") -> HudTextSource.TARGET_NAME
            "targetname" in normalized && "targethp" in normalized && "targetmaxhp" in normalized ->
                HudTextSource.TARGET_HEALTH_SUMMARY
            else -> null
        }
    }

    private fun staticText(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.length >= 2 && trimmed.first() == '"' && trimmed.last() == '"') {
            return trimmed.substring(1, trimmed.lastIndex)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
        }
        return trimmed.takeIf {
            it.any { char -> char.isWhitespace() } && it.none { char -> char in "(){}[]=<>!+*/?:" }
        }
    }

    private fun dimension(node: JsonElement?, path: String): Int? {
        val value = staticDoubleOrNull(node)
        if (node != null && value == null) warning(path, "Deferred dynamic legacy dimension '${expression(node).orEmpty().take(120)}'")
        return value?.roundToInt()
    }

    private fun staticDouble(node: JsonElement?, path: String, default: Double): Double =
        staticDoubleOrNull(node) ?: run {
            if (node != null && node !is JsonNull) warning(path, "Deferred dynamic legacy value '${expression(node).orEmpty().take(120)}'")
            default
        }

    private fun staticDoubleOrNull(node: JsonElement?): Double? = expression(node)?.trim()?.toDoubleOrNull()

    private fun staticInt(node: JsonElement?, path: String, default: Int?): Int? {
        val value = expression(node)?.trim()?.let { raw ->
            raw.toIntOrNull() ?: raw.toDoubleOrNull()?.roundToInt()
        }
        if (node != null && value == null) warning(path, "Deferred dynamic legacy integer '${expression(node).orEmpty().take(120)}'")
        return value ?: default
    }

    private fun staticBoolean(node: JsonElement?, path: String, default: Boolean): Boolean {
        val value = expression(node)?.trim()?.toBooleanStrictOrNull()
        if (node != null && value == null) warning(path, "Deferred dynamic legacy boolean '${expression(node).orEmpty().take(120)}'")
        return value ?: default
    }

    private fun color(node: JsonElement?, path: String): ArgbColorDefinition? {
        if (node == null || node is JsonNull) return null
        val raw = expression(node)?.trim().orEmpty()
        val rgba = when {
            raw.startsWith("0x", true) -> raw.substring(2).toLongOrNull(16)
            raw.startsWith("#") -> raw.substring(1).toLongOrNull(16)
            else -> raw.toLongOrNull()
        }
        if (rgba == null) {
            warning(path, "Deferred dynamic legacy RGBA expression '${raw.take(120)}'; using default tint")
            return null
        }
        val normalized = if (raw.removePrefix("#").removePrefix("0x").removePrefix("0X").length <= 6) {
            0xFF000000L or (rgba and 0xFFFFFF)
        } else {
            ((rgba and 0xFF) shl 24) or ((rgba ushr 8) and 0xFFFFFF)
        }
        return ArgbColorDefinition(normalized.toInt())
    }

    private fun selectedColors(raw: String?): Pair<ArgbColorDefinition, ArgbColorDefinition>? {
        if (raw == null) return null
        val colors = Regex("0x([0-9a-fA-F]{8})").findAll(raw).map { legacyRgba(it.groupValues[1]) }.toList()
        return if (colors.size >= 2) colors[0] to colors[1] else null
    }

    private fun legacyRgba(hex: String): ArgbColorDefinition {
        val rgba = hex.toLong(16)
        return ArgbColorDefinition((((rgba and 0xFF) shl 24) or ((rgba ushr 8) and 0xFFFFFF)).toInt())
    }

    private fun linearIndexExpression(raw: String?): Pair<Double, Double>? {
        if (raw == null || 'i' !in raw) return null
        val normalized = raw.lowercase().replace(" ", "")
        Regex("^([+-]?\\d+(?:\\.\\d+)?)\\*i$").matchEntire(normalized)?.let {
            return 0.0 to (it.groupValues[1].toDoubleOrNull() ?: return null)
        }
        Regex("^([+-]?\\d+(?:\\.\\d+)?)([+-]\\d+(?:\\.\\d+)?)\\*i$").matchEntire(normalized)?.let {
            return (it.groupValues[1].toDoubleOrNull() ?: return null) to
                (it.groupValues[2].toDoubleOrNull() ?: return null)
        }
        return null
    }

    private fun expression(node: JsonElement?): String? = when (node) {
        null, JsonNull -> null
        is JsonPrimitive -> node.contentOrNull
        is JsonObject -> node["expression"]?.jsonPrimitive?.contentOrNull
        else -> null
    }

    private fun stringValue(node: JsonElement?): String? = (node as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.withName(name: String?): JsonObject =
        if (name == null || "name" in this) this else JsonObject(this + ("name" to JsonPrimitive(name)))

    private fun warning(field: String, message: String) {
        issues += ThemeIssue(ThemeIssueSeverity.WARNING, resource, themeId, field, message)
    }

    private data class Axis(val alignment: Alignment, val offset: Double)
    private enum class Alignment { START, CENTER, END }

    private companion object {
        val HOTBAR_ITEM_TYPES = setOf("glhotbaritem", "hotbaritem")
        val PROGRESS_PATTERN = Regex(
            "^(\\d+(?:\\.\\d+)?)\\*(?:player\\.)?(healthPercent|hpPct|foodPercent|foodPct|airPercent|airPct|experience|horseJump|targetHpPct)$",
            RegexOption.IGNORE_CASE,
        )
    }
}
