package io.github.myl7.chestlink;

import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ChestLinkCommand {
	private static final double REACH = 5.0;

	private ChestLinkCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("chestlink")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("link")
						.then(Commands.argument("channel", StringArgumentType.string())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										LinkManager.channelNames().stream().map(StringArgumentType::escapeIfRequired),
										builder))
								.executes(context -> link(context.getSource(),
										StringArgumentType.getString(context, "channel")))))
				.then(Commands.literal("unlink")
						.executes(context -> unlink(context.getSource())))
				.then(Commands.literal("list")
						.executes(context -> list(context.getSource()))));
	}

	private static int link(CommandSourceStack source, String channel) throws CommandSyntaxException {
		ServerLevel level = source.getLevel();
		ChestBlockEntity chest = targetedChest(source);
		if (chest == null) {
			return 0;
		}

		BlockPos pos = chest.getBlockPos();
		BlockState blockState = level.getBlockState(pos);
		if (!(blockState.getBlock() instanceof ChestBlock)
				|| blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			source.sendFailure(Component.literal("Only single chests can be linked, not double chests"));
			return 0;
		}

		if (chest.getLootTable() != null) {
			source.sendFailure(Component.literal("This chest has an unopened loot table and cannot be linked"));
			return 0;
		}

		String existing = LinkManager.channelName(level, pos);
		if (existing != null) {
			source.sendFailure(Component.literal("This chest is already linked to channel '" + existing + "'"));
			return 0;
		}

		boolean channelExists = LinkManager.hasChannel(channel);
		if (channelExists && !chest.isEmpty()) {
			source.sendFailure(Component.literal(
					"Channel '" + channel + "' already exists; empty this chest first before linking it"));
			return 0;
		}

		LinkManager.link(level, chest, channel);
		LinkManager.burst(level, pos);
		source.sendSuccess(() -> Component.literal(channelExists
				? "Linked chest to channel '" + channel + "'"
				: "Created channel '" + channel + "' with this chest's contents"), true);
		return 1;
	}

	private static int unlink(CommandSourceStack source) throws CommandSyntaxException {
		ServerLevel level = source.getLevel();
		ChestBlockEntity chest = targetedChest(source);
		if (chest == null) {
			return 0;
		}

		BlockPos pos = chest.getBlockPos();
		String channel = LinkManager.unlink(level, pos, chest);
		if (channel == null) {
			source.sendFailure(Component.literal("This chest is not linked to any channel"));
			return 0;
		}

		LinkManager.burst(level, pos);
		source.sendSuccess(() -> Component.literal("Unlinked chest from channel '" + channel + "'"), true);
		return 1;
	}

	private static int list(CommandSourceStack source) {
		Map<String, LinkState.Channel> channels = LinkManager.channels();
		if (channels.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No chest link channels exist"), false);
			return 0;
		}

		channels.forEach((name, channel) -> {
			String members = channel.members.stream()
					.map(pos -> pos.dimension().identifier() + " [" + pos.pos().getX() + ", "
							+ pos.pos().getY() + ", " + pos.pos().getZ() + "]")
					.collect(Collectors.joining(", "));
			source.sendSuccess(() -> Component.literal(
					name + " (" + channel.members.size() + "): " + members), false);
		});
		return channels.size();
	}

	/**
	 * The chest block entity the executing player is looking at within 5 blocks,
	 * or null after sending a failure message.
	 */
	private static ChestBlockEntity targetedChest(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();

		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(REACH));
		BlockHitResult hit = level.clip(new ClipContext(
				eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

		if (hit.getType() != HitResult.Type.BLOCK
				|| !(level.getBlockEntity(hit.getBlockPos()) instanceof ChestBlockEntity chest)) {
			source.sendFailure(Component.literal("Look at a chest within " + (int) REACH + " blocks"));
			return null;
		}

		return chest;
	}
}
