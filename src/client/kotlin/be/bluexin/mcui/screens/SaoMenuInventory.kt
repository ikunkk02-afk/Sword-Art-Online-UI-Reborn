/* SPDX-License-Identifier: GPL-3.0-or-later */
package be.bluexin.mcui.screens

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.*
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.entity.player.StackedContents
import net.minecraft.client.RecipeBookCategories

/** 1.12.2 BaseFilters categories, using 1.21.1 slot acceptance and item types. */
enum class SaoItemCategory(val translation: String, val icon: SaoIcon, val parent: SaoItemCategory? = null, val equipSlot: Int? = null) {
    EQUIPMENT("sao.element.equipment", SaoIcon.EQUIPMENT),
    ARMOR("sao.element.armor", SaoIcon.ARMOR, EQUIPMENT),
    HELMET("sao.element.helmet", SaoIcon.ARMOR, ARMOR, 5),
    CHEST("sao.element.chestplates", SaoIcon.ARMOR, ARMOR, 6),
    LEGS("sao.element.leggings", SaoIcon.ARMOR, ARMOR, 7),
    BOOTS("sao.element.boots", SaoIcon.ARMOR, ARMOR, 8),
    SHIELDS("sao.element.shields", SaoIcon.ARMOR, ARMOR, 45),
    WEAPONS("sao.element.weapons", SaoIcon.EQUIPMENT, EQUIPMENT),
    SWORDS("sao.element.swords", SaoIcon.EQUIPMENT, WEAPONS),
    BOWS("sao.element.bows", SaoIcon.EQUIPMENT, WEAPONS),
    TOOLS("sao.element.tools", SaoIcon.EQUIPMENT, EQUIPMENT),
    PICKAXES("sao.element.pickaxe", SaoIcon.EQUIPMENT, TOOLS),
    AXES("sao.element.axe", SaoIcon.EQUIPMENT, TOOLS),
    SHOVELS("sao.element.shovel", SaoIcon.EQUIPMENT, TOOLS),
    OTHER_TOOLS("sao.element.compattools", SaoIcon.EQUIPMENT, TOOLS),
    ACCESSORIES("sao.element.accessories", SaoIcon.EQUIPMENT, EQUIPMENT),
    ITEMS("sao.element.items", SaoIcon.ITEMS),
    CONSUMABLES("sao.element.consumables", SaoIcon.ITEMS, ITEMS),
    BLOCKS("sao.element.blocks", SaoIcon.ITEMS, ITEMS),
    MATERIALS("sao.element.materials", SaoIcon.ITEMS, ITEMS);

    fun accepts(stack: ItemStack): Boolean = when (this) {
        HELMET, CHEST, LEGS, BOOTS -> Minecraft.getInstance().player!!.inventoryMenu.getSlot(equipSlot!!).mayPlace(stack)
        SHIELDS -> stack.item is ShieldItem
        SWORDS -> stack.item is SwordItem || stack.item is TridentItem
        BOWS -> stack.item is ProjectileWeaponItem
        PICKAXES -> stack.item is PickaxeItem
        AXES -> stack.item is AxeItem
        SHOVELS -> stack.item is ShovelItem
        OTHER_TOOLS -> stack.item is HoeItem || stack.item is ShearsItem
        ACCESSORIES -> Minecraft.getInstance().player?.inventoryMenu?.slots?.withIndex()?.any { (menuIndex, slot) ->
            menuIndex > 45 && slot.mayPlace(stack)
        } == true
        CONSUMABLES -> stack.useAnimation == UseAnim.EAT || stack.useAnimation == UseAnim.DRINK
        BLOCKS -> stack.item is BlockItem
        MATERIALS -> entries.filter { it.parent != null && it != MATERIALS }.none { it.accepts(stack) }
        else -> false
    }
}

/** All inventory mutations go through the vanilla server-validated container protocol. */
object SaoMenuInventory {
    private val client get() = Minecraft.getInstance()

    fun items(category: SaoItemCategory): List<Pair<Int, ItemStack>> = client.player?.inventoryMenu?.slots.orEmpty()
        .withIndex()
        .filter { (menuIndex, slot) -> menuIndex >= 5 && slot.hasItem() && category.accepts(slot.item) }
        .map { (menuIndex, slot) -> menuIndex to slot.item.copy() }

    private fun equipDestination(category: SaoItemCategory, stack: ItemStack): Int? =
        category.equipSlot ?: if (category == SaoItemCategory.ACCESSORIES) {
            client.player?.inventoryMenu?.slots?.withIndex()?.firstOrNull { (menuIndex, slot) ->
                menuIndex > 45 && slot.mayPlace(stack)
            }?.index
        } else null

    private fun current(slot: Int, expected: ItemStack): Boolean {
        val player = client.player ?: return false
        return slot in 5 until player.inventoryMenu.slots.size && pending == null &&
            player.containerMenu === player.inventoryMenu && player.inventoryMenu.carried.isEmpty &&
            player.inventoryMenu.getSlot(slot).mayPickup(player) &&
            ItemStack.matches(player.inventoryMenu.getSlot(slot).item, expected)
    }

    private fun click(slot: Int, button: Int, type: ClickType) {
        val player = client.player ?: return
        client.gameMode?.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, button, type, player)
    }

    fun inspect(parent: SaoIngameMenuScreen, slot: Int, expected: ItemStack, category: SaoItemCategory) {
        parent.preparePopupReturn()
        fun back() { parent.preparePopupReturn(); client.setScreen(parent) }
        client.setScreen(LegacyPopupScreen(parent, expected.hoverName,
            expected.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL), Component.empty(),
            listOf(
                PopupButton(SaoIcon.EQUIPMENT, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER) {
                    if (!current(slot, expected)) back()
                    else if (equipDestination(category, expected) != null) {
                        val destination = requireNotNull(equipDestination(category, expected))
                        if (slot == destination) click(slot, 0, ClickType.QUICK_MOVE)
                        else {
                            val player = client.player!!
                            val sourceSlot = player.inventoryMenu.getSlot(slot)
                            val targetSlot = player.inventoryMenu.getSlot(destination)
                            if (targetSlot.mayPickup(player) && targetSlot.mayPlace(expected) &&
                                expected.count <= targetSlot.getMaxStackSize(expected) &&
                                (targetSlot.item.isEmpty || (sourceSlot.mayPlace(targetSlot.item) &&
                                    targetSlot.item.count <= sourceSlot.getMaxStackSize(targetSlot.item)))) {
                                click(destination, 0, ClickType.PICKUP)
                                click(slot, 0, ClickType.PICKUP)
                                click(destination, 0, ClickType.PICKUP)
                            }
                        }
                        back()
                    } else {
                        parent.preparePopupReturn()
                        client.setScreen(LegacyPopupScreen(parent, expected.hoverName,
                            listOf(Component.translatable("mcui.inventory.select_hotbar")), Component.empty(),
                            (0..8).map { hotbar -> PopupButton(SaoIcon.ITEMS, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER,
                                action = {
                                    if (current(slot, expected)) click(slot, hotbar, ClickType.SWAP)
                                    back()
                                }, label = (hotbar + 1).toString()) }))
                    }
                },
                PopupButton(SaoIcon.CANCEL, LegacySaoMetrics.CANCEL, LegacySaoMetrics.CANCEL_HOVER) {
                    parent.preparePopupReturn()
                    client.setScreen(SaoConfirmationScreen(parent, expected.hoverName,
                        Component.translatable("mcui.inventory.discard")) {
                        if (current(slot, expected)) click(slot, 0, ClickType.THROW)
                        back()
                    })
                },
            )))
    }

    fun recipes(): List<RecipeHolder<CraftingRecipe>> {
        val player = client.player ?: return emptyList()
        val contents = StackedContents()
        player.inventory.fillStackedContents(contents)
        return client.level!!.recipeManager.getAllRecipesFor(RecipeType.CRAFTING).filter {
            player.recipeBook.contains(it) && it.value.canCraftInDimensions(2, 2) &&
                !it.value.getResultItem(player.registryAccess()).isEmpty && contents.canCraft(it.value, null)
        }
    }

    data class RecipeGroup(val translation: String, val recipes: List<RecipeHolder<CraftingRecipe>>)

    fun recipeGroups(): List<RecipeGroup> {
        val player = client.player ?: return emptyList()
        val contents = StackedContents().also { player.inventory.fillStackedContents(it) }
        val categories = listOf(
            RecipeBookCategories.CRAFTING_BUILDING_BLOCKS to "mcui.crafting.category.building",
            RecipeBookCategories.CRAFTING_REDSTONE to "mcui.crafting.category.redstone",
            RecipeBookCategories.CRAFTING_EQUIPMENT to "mcui.crafting.category.equipment",
            RecipeBookCategories.CRAFTING_MISC to "mcui.crafting.category.misc",
        )
        return categories.mapNotNull { (category, translation) ->
            val recipes = player.recipeBook.getCollection(category).flatMap { collection ->
                collection.updateKnownRecipes(player.recipeBook)
                collection.canCraft(contents, 2, 2, player.recipeBook)
                collection.getRecipes(true)
            }.mapNotNull { holder ->
                @Suppress("UNCHECKED_CAST")
                (holder.takeIf { it.value is CraftingRecipe } as? RecipeHolder<CraftingRecipe>)
            }.filter { it.value.canCraftInDimensions(2, 2) }.distinctBy { it.id }
            RecipeGroup(translation, recipes).takeIf { it.recipes.isNotEmpty() }
        }
    }

    private var pending: RecipeHolder<CraftingRecipe>? = null
    private var craftingPlayer: net.minecraft.client.player.LocalPlayer? = null
    private var waited = 0
    private var remaining = 0
    private var awaitingNext = false
    val isCrafting get() = pending != null

    private fun maxCrafts(recipe: RecipeHolder<CraftingRecipe>): Int {
        val player = client.player ?: return 0
        val contents = StackedContents()
        player.inventory.fillStackedContents(contents)
        return contents.getBiggestCraftableStack(recipe, null)
    }

    fun craft(parent: SaoIngameMenuScreen, recipe: RecipeHolder<CraftingRecipe>) {
        val player = client.player ?: return
        val result = recipe.value.getResultItem(player.registryAccess())
        if (result.isEmpty) return
        var count = 1
        parent.preparePopupReturn()
        client.setScreen(LegacyPopupScreen(parent, result.hoverName,
            result.getTooltipLines(Item.TooltipContext.of(client.level), player, TooltipFlag.NORMAL), Component.empty(),
            listOf(-10, -1, 0, 1, 10).map { delta ->
                PopupButton(SaoIcon.CONFIRM, LegacySaoMetrics.CONFIRM, LegacySaoMetrics.CONFIRM_HOVER,
                    label = if (delta == 0) null else delta.toString(), closeOnClick = delta == 0, action = {
                        if (delta != 0) count = (count + delta).coerceIn(1, maxCrafts(recipe).coerceAtLeast(1))
                        else {
                            if (client.player === player && pending == null && player.containerMenu === player.inventoryMenu &&
                                player.inventoryMenu.carried.isEmpty && (1..4).all { player.inventoryMenu.getSlot(it).item.isEmpty } &&
                                recipe in recipes() && maxCrafts(recipe) >= count) {
                                pending = recipe
                                craftingPlayer = player
                                remaining = count
                                awaitingNext = false
                                waited = 0
                                client.gameMode?.handlePlaceRecipe(player.inventoryMenu.containerId, recipe, false)
                            } else craftingNotice("mcui.crafting.unavailable")
                            parent.preparePopupReturn()
                            client.setScreen(parent)
                        }
                    })
            }, footerSupplier = { Component.translatable("mcui.crafting.quantity", count * result.count, result.hoverName) }))
    }

    fun cancelCraft() {
        val player = client.player
        if (pending != null && player != null && player === craftingPlayer &&
            player.containerMenu === player.inventoryMenu && player.inventoryMenu.carried.isEmpty) {
            // Only return our own pending ingredients, never another container's slots.
            (1..4).forEach { click(it, 0, ClickType.QUICK_MOVE) }
        }
        pending = null
        craftingPlayer = null
        remaining = 0
        awaitingNext = false
    }

    fun tick() {
        val recipe = pending ?: return
        val player = client.player
        if (player == null || player !== craftingPlayer || player.containerMenu !== player.inventoryMenu) {
            pending = null; craftingPlayer = null; return
        }
        if (++waited > 100 || !player.inventoryMenu.carried.isEmpty) {
            cancelCraft(); craftingNotice("mcui.crafting.stopped"); return
        }
        if (awaitingNext) {
            if (waited < 5) return
            if (remaining <= 0) { cancelCraft(); craftingNotice("mcui.crafting.complete"); return }
            // A recipe can leave buckets/bottles: return them before placing the next batch.
            (1..4).forEach { if (player.inventoryMenu.getSlot(it).hasItem()) click(it, 0, ClickType.QUICK_MOVE) }
            if ((1..4).any { player.inventoryMenu.getSlot(it).hasItem() } || maxCrafts(recipe) == 0) {
                cancelCraft(); craftingNotice("mcui.crafting.stopped"); return
            }
            client.gameMode?.handlePlaceRecipe(player.inventoryMenu.containerId, recipe, false)
            awaitingNext = false
            waited = 0
            return
        }
        val result = player.inventoryMenu.getSlot(0).item
        if (waited >= 5 && !result.isEmpty && ItemStack.isSameItemSameComponents(result, recipe.value.getResultItem(player.registryAccess()))) {
            // Inventory-only recipe placement requested ONE operation, never a shift-filled grid.
            val expected = result.copy()
            fun storedCount() = (9..44).sumOf { index -> player.inventoryMenu.getSlot(index).item.let {
                if (ItemStack.isSameItemSameComponents(it, expected)) it.count else 0
            } }
            val before = storedCount()
            click(0, 0, ClickType.QUICK_MOVE)
            if (storedCount() - before != expected.count) {
                cancelCraft(); craftingNotice("mcui.crafting.stopped"); return
            }
            remaining--
            awaitingNext = true
            waited = 0
        }
    }

    private fun craftingNotice(key: String) = SaoNotificationAlert.show(SaoIcon.CRAFTING,
        Component.translatable("guiCrafting"), Component.translatable(key))
}
