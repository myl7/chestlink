package io.github.myl7.chestlink;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runtime index over {@link LinkState}. All access happens on the server main
 * thread (commands, hoppers, block updates all tick there), so no locking.
 */
public final class LinkManager {
	private static MinecraftServer server;
	private static LinkState state;
	private static final Map<GlobalPos, String> BY_POS = new HashMap<>();
	private static int tickCounter;

	private LinkManager() {
	}

	public static void load(MinecraftServer startedServer) {
		server = startedServer;
		state = startedServer.getDataStorage().computeIfAbsent(LinkState.TYPE);
		BY_POS.clear();
		state.channels().forEach((name, channel) -> channel.members.forEach(pos -> BY_POS.put(pos, name)));
		tickCounter = 0;
	}

	public static void unload() {
		server = null;
		state = null;
		BY_POS.clear();
	}

	/**
	 * The shared item list for a linked chest, or null when the position is not
	 * linked (or we are not on a running server). This is the single interception
	 * point every read/write path funnels through.
	 */
	public static NonNullList<ItemStack> sharedItems(Level level, BlockPos pos) {
		if (state == null || level.isClientSide()) {
			return null;
		}

		String name = BY_POS.get(new GlobalPos(level.dimension(), pos));
		if (name == null) {
			return null;
		}

		LinkState.Channel channel = state.channels().get(name);
		return channel == null ? null : channel.items;
	}

	public static String channelName(ServerLevel level, BlockPos pos) {
		return state == null ? null : BY_POS.get(new GlobalPos(level.dimension(), pos));
	}

	public static boolean hasChannel(String name) {
		return state != null && state.channels().containsKey(name);
	}

	public static Set<String> channelNames() {
		return state == null ? Set.of() : state.channels().keySet();
	}

	public static Map<String, LinkState.Channel> channels() {
		return state == null ? Map.of() : state.channels();
	}

	/**
	 * Links a chest into a channel. Validation (single chest, no loot table, not
	 * already linked, empty when joining an existing channel) happens in the
	 * command. A new channel is seeded with the chest's current contents; in both
	 * cases the chest's own backing list is cleared afterwards, since the channel
	 * now owns the items.
	 */
	public static void link(ServerLevel level, ChestBlockEntity chest, String name) {
		if (state == null) {
			return;
		}

		GlobalPos pos = new GlobalPos(level.dimension(), chest.getBlockPos());
		LinkState.Channel channel = state.channels().get(name);

		if (channel == null) {
			channel = new LinkState.Channel();
			// Not linked yet, so these reads still hit the chest's own storage.
			for (int i = 0; i < Math.min(LinkState.CHANNEL_SIZE, chest.getContainerSize()); i++) {
				channel.items.set(i, chest.getItem(i));
			}
			state.channels().put(name, channel);
		}

		channel.members.add(pos);
		BY_POS.put(pos, name);
		((ChestLinkChest) chest).chestlink$setOwnItems(NonNullList.withSize(LinkState.CHANNEL_SIZE, ItemStack.EMPTY));
		state.setDirty();
	}

	/**
	 * Unlinks a chest, shared by the command and the block-destruction hook.
	 * Returns the channel name, or null when the position was not linked.
	 *
	 * <p>Non-last member: the chest's own backing list is cleared. It may hold a
	 * stale snapshot of the shared contents written by an earlier save, and leaving
	 * it in place would duplicate the channel contents. The items stay in the
	 * channel. Last member: the shared contents are copied into the chest's own
	 * list and the channel is deleted, so the items stay in this chest (and drop
	 * from it if it is being destroyed).
	 */
	public static String unlink(ServerLevel level, BlockPos blockPos, ChestBlockEntity chest) {
		if (state == null) {
			return null;
		}

		GlobalPos pos = new GlobalPos(level.dimension(), blockPos);
		String name = BY_POS.remove(pos);
		if (name == null) {
			return null;
		}

		LinkState.Channel channel = state.channels().get(name);
		if (channel != null) {
			channel.members.remove(pos);

			NonNullList<ItemStack> own = NonNullList.withSize(LinkState.CHANNEL_SIZE, ItemStack.EMPTY);
			if (channel.members.isEmpty()) {
				state.channels().remove(name);
				for (int i = 0; i < LinkState.CHANNEL_SIZE; i++) {
					own.set(i, channel.items.get(i));
				}
			}
			((ChestLinkChest) chest).chestlink$setOwnItems(own);
		}

		state.setDirty();
		return name;
	}

	/** Called from the LevelChunk hook right before vanilla drops the chest's contents. */
	public static void beforeChestRemoved(ChestBlockEntity chest) {
		if (state != null && chest.getLevel() instanceof ServerLevel level) {
			unlink(level, chest.getBlockPos(), chest);
		}
	}

	/**
	 * Called after {@code BlockEntity#setChanged} on any chest. Marks the persistent
	 * state dirty and refreshes comparators next to the other loaded members.
	 * Vanilla already updates comparators at the changed chest itself.
	 */
	public static void onChestChanged(ChestBlockEntity chest) {
		Level level = chest.getLevel();
		if (state == null || server == null || level == null || level.isClientSide()) {
			return;
		}

		GlobalPos pos = new GlobalPos(level.dimension(), chest.getBlockPos());
		String name = BY_POS.get(pos);
		if (name == null) {
			return;
		}

		state.setDirty();

		LinkState.Channel channel = state.channels().get(name);
		if (channel == null) {
			return;
		}

		for (GlobalPos member : channel.members) {
			if (member.equals(pos)) {
				continue;
			}

			ServerLevel memberLevel = server.getLevel(member.dimension());
			if (memberLevel != null && memberLevel.isLoaded(member.pos())) {
				BlockState memberState = memberLevel.getBlockState(member.pos());
				memberLevel.updateNeighbourForOutputSignal(member.pos(), memberState.getBlock());
			}
		}
	}

	/** Ambient reverse-portal particles above every loaded linked chest, once a second. */
	public static void tick(MinecraftServer tickingServer) {
		if (state == null || ++tickCounter < 20) {
			return;
		}

		tickCounter = 0;
		for (LinkState.Channel channel : state.channels().values()) {
			for (GlobalPos member : channel.members) {
				ServerLevel level = tickingServer.getLevel(member.dimension());
				if (level == null || !level.isLoaded(member.pos())) {
					continue;
				}

				BlockPos pos = member.pos();
				level.sendParticles(ParticleTypes.REVERSE_PORTAL,
						pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
						2, 0.25, 0.1, 0.25, 0.01);
			}
		}
	}

	/** One-shot portal burst as link/unlink feedback, visible to vanilla clients. */
	public static void burst(ServerLevel level, BlockPos pos) {
		level.sendParticles(ParticleTypes.PORTAL,
				pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
				30, 0.3, 0.3, 0.3, 0.5);
	}
}
