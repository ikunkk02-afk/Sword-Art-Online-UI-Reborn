package be.bluexin.mcui.config

import java.nio.file.Files
import java.util.UUID
import kotlin.test.*

class SaoFriendStoreTest {
    @Test fun `friends persist and renaming does not duplicate UUID`() {
        val directory = Files.createTempDirectory("sao-friends-test")
        val file = directory.resolve("friends.json")
        try {
            val uuid = UUID.randomUUID()
            val store = SaoFriendStore(file)
            store.load()
            store.add(uuid, "OldName")
            store.add(uuid, "NewName")
            val reloaded = SaoFriendStore(file).also { it.load() }
            assertEquals(listOf(SaoFriend(uuid.toString(), "NewName")), reloaded.friends)
            reloaded.remove(uuid)
            assertTrue(SaoFriendStore(file).also { it.load() }.friends.isEmpty())
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory) }
    }

    @Test fun `invalid file is preserved and cannot silently be overwritten`() {
        val directory = Files.createTempDirectory("sao-friends-test")
        val file = directory.resolve("friends.json")
        try {
            Files.writeString(file, "invalid original content")
            val store = SaoFriendStore(file)
            assertFails { store.load() }
            assertFails { store.add(UUID.randomUUID(), "Player") }
            assertEquals("invalid original content", Files.readString(file))
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory) }
    }

    @Test fun `legacy Forge friend list migrates UUIDs names and quoted names`() {
        val directory = Files.createTempDirectory("sao-friends-migrate-test")
        val file = directory.resolve("friends.json")
        val legacy = directory.resolve("friend_list.cfg")
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()
        try {
            Files.writeString(legacy, """
                Friends {
                    S:Alice=$first
                    S:"Name With Spaces"=$second
                }
            """.trimIndent())
            val store = SaoFriendStore(file, legacy).also { it.load() }
            assertEquals(listOf(SaoFriend(first.toString(), "Alice"), SaoFriend(second.toString(), "Name With Spaces")), store.friends)
            assertTrue(Files.exists(legacy), "Migration must preserve the recoverable source file")
            assertTrue(Files.exists(file))
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(legacy); Files.deleteIfExists(directory) }
    }
}
