package io.github.myl7.chestlink;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistent storage for all chest link channels. Attached to the server-global
 * {@code SavedDataStorage}, so it is dimension-independent and lives in
 * {@code <world>/data/chestlink/links.dat}.
 */
public class LinkState extends SavedData {
	public static final int CHANNEL_SIZE = 27;

	private static final Codec<Channel> CHANNEL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(channel -> List.copyOf(channel.items)),
			GlobalPos.CODEC.listOf().fieldOf("members").forGetter(channel -> List.copyOf(channel.members))
	).apply(instance, Channel::fromLists));

	public static final Codec<LinkState> CODEC = Codec.unboundedMap(Codec.STRING, CHANNEL_CODEC)
			.xmap(LinkState::new, state -> Map.copyOf(state.channels));

	public static final SavedDataType<LinkState> TYPE = new SavedDataType<>(
			Identifier.fromNamespaceAndPath(ChestLinkMod.MOD_ID, "links"),
			LinkState::new,
			CODEC,
			null
	);

	private final Map<String, Channel> channels = new LinkedHashMap<>();

	public LinkState() {
	}

	private LinkState(Map<String, Channel> loaded) {
		this.channels.putAll(loaded);
	}

	public Map<String, Channel> channels() {
		return this.channels;
	}

	/**
	 * One channel: a single shared 27-slot inventory plus the set of member chests.
	 * The {@code items} list instance is what every linked chest reads and writes, so
	 * it must never be replaced, only mutated.
	 */
	public static final class Channel {
		public final NonNullList<ItemStack> items = NonNullList.withSize(CHANNEL_SIZE, ItemStack.EMPTY);
		public final Set<GlobalPos> members = new LinkedHashSet<>();

		private static Channel fromLists(List<ItemStack> items, List<GlobalPos> members) {
			Channel channel = new Channel();
			for (int i = 0; i < Math.min(CHANNEL_SIZE, items.size()); i++) {
				channel.items.set(i, items.get(i));
			}
			channel.members.addAll(members);
			return channel;
		}
	}
}
