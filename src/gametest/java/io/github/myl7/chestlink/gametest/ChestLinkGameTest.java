package io.github.myl7.chestlink.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import io.github.myl7.chestlink.LinkManager;

public final class ChestLinkGameTest {
	@GameTest
	public void linkedChestsShareOneInventory(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos firstPos = new BlockPos(1, 1, 1);
		BlockPos secondPos = new BlockPos(3, 1, 1);
		ChestBlockEntity first = placeChest(helper, firstPos);
		ChestBlockEntity second = placeChest(helper, secondPos);
		String channel = channelName(helper, "shared");

		first.setItem(0, new ItemStack(Items.DIAMOND, 3));
		LinkManager.link(level, first, channel);
		LinkManager.link(level, second, channel);
		assertStack(helper, second.getItem(0), Items.DIAMOND, 3,
				"the second member should see the first chest's contents");

		second.setItem(0, new ItemStack(Items.EMERALD, 7));
		assertStack(helper, first.getItem(0), Items.EMERALD, 7,
				"a write through one member should be visible through the other");

		LinkManager.unlink(level, first.getBlockPos(), first);
		LinkManager.unlink(level, second.getBlockPos(), second);
		helper.succeed();
	}

	@GameTest
	public void unlinkingANonLastMemberDoesNotDuplicateItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChestBlockEntity first = placeChest(helper, new BlockPos(1, 1, 1));
		ChestBlockEntity second = placeChest(helper, new BlockPos(3, 1, 1));
		String channel = channelName(helper, "non-last");

		first.setItem(4, new ItemStack(Items.GOLD_INGOT, 12));
		LinkManager.link(level, first, channel);
		LinkManager.link(level, second, channel);
		LinkManager.unlink(level, first.getBlockPos(), first);

		helper.assertTrue(first.isEmpty(), "the removed non-last member should have an empty private inventory");
		helper.assertTrue(LinkManager.hasChannel(channel), "the channel should remain while another member exists");
		assertStack(helper, second.getItem(4), Items.GOLD_INGOT, 12,
				"the channel inventory should remain with the linked member");

		LinkManager.unlink(level, second.getBlockPos(), second);
		helper.succeed();
	}

	@GameTest
	public void unlinkingTheLastMemberRestoresTheInventory(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ChestBlockEntity chest = placeChest(helper, new BlockPos(1, 1, 1));
		String channel = channelName(helper, "last");

		chest.setItem(8, new ItemStack(Items.REDSTONE, 32));
		LinkManager.link(level, chest, channel);
		LinkManager.unlink(level, chest.getBlockPos(), chest);

		helper.assertFalse(LinkManager.hasChannel(channel), "removing the last member should delete the channel");
		assertStack(helper, chest.getItem(8), Items.REDSTONE, 32,
				"the last member should receive the channel inventory");
		helper.succeed();
	}

	private static ChestBlockEntity placeChest(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.CHEST);
		return helper.getBlockEntity(pos, ChestBlockEntity.class);
	}

	private static String channelName(GameTestHelper helper, String suffix) {
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		return "gametest-" + suffix + "-" + origin.getX() + "-" + origin.getZ();
	}

	private static void assertStack(
			GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
		helper.assertTrue(stack.getItem() == item && stack.getCount() == count, message);
	}
}
