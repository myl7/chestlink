package io.github.myl7.chestlink;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * Implemented on {@code ChestBlockEntity} by mixin. Writes the chest's own backing
 * item list, bypassing the shared-channel interception on {@code getItems}. Needed
 * when linking/unlinking to clear or restore the chest's private storage.
 */
public interface ChestLinkChest {
	void chestlink$setOwnItems(NonNullList<ItemStack> items);
}
