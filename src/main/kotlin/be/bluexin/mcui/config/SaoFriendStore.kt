/* SPDX-License-Identifier: GPL-3.0-or-later */
package be.bluexin.mcui.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

@Serializable
data class SaoFriend(val uuid: String, val name: String)

/** The legacy friend list is local; UUID, not display name, identifies a friend. */
class SaoFriendStore(private val file: Path, private val legacyFile: Path? = null) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    var friends: List<SaoFriend> = emptyList()
        private set
    private var writable = true

    fun load() {
        try {
            if (!Files.exists(file) && legacyFile?.let(Files::exists) == true) {
                save(parseLegacy(Files.readAllLines(legacyFile)))
                return
            }
            val loaded = if (Files.exists(file)) json.decodeFromString<List<SaoFriend>>(Files.readString(file)) else emptyList()
            loaded.forEach { UUID.fromString(it.uuid); require(it.name.isNotBlank()) }
            friends = loaded.distinctBy { UUID.fromString(it.uuid) }
            writable = true
        } catch (exception: Exception) {
            writable = false // Never overwrite a malformed list with a fresh, empty one.
            throw exception
        }
    }

    fun add(uuid: UUID, name: String) {
        require(name.isNotBlank())
        save(friends.filterNot { UUID.fromString(it.uuid) == uuid } + SaoFriend(uuid.toString(), name))
    }

    fun remove(uuid: UUID) = save(friends.filterNot { UUID.fromString(it.uuid) == uuid })

    private fun save(value: List<SaoFriend>) {
        check(writable) { "Friend list could not be loaded; existing file preserved" }
        val directory = file.toAbsolutePath().parent
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "sao-friends-", ".tmp")
        try {
            Files.writeString(temporary, json.encodeToString(value))
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
            }
            friends = value
        } finally { Files.deleteIfExists(temporary) }
    }

    private fun parseLegacy(lines: List<String>): List<SaoFriend> {
        val property = Regex("""^\s*S:(?:\"([^\"]+)\"|([^=]+))=([0-9a-fA-F-]{36})\s*$""")
        return lines.mapNotNull { line ->
            val match = property.matchEntire(line) ?: return@mapNotNull null
            val name = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
            runCatching { SaoFriend(UUID.fromString(match.groupValues[3]).toString(), name) }.getOrNull()
                ?.takeIf { it.name.isNotBlank() }
        }.distinctBy { it.uuid }
    }
}
