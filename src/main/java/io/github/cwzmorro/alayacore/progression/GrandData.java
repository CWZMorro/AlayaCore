package io.github.cwzmorro.alayacore.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * Grand Servant titles per class (plan 13.4). Plain Java, no game calls. Immutable.
 *
 * @param qualified per class, the players who qualified, earliest first
 * @param titles    per class, the players holding the title
 */
public record GrandData(Map<Identifier, List<UUID>> qualified, Map<Identifier, List<UUID>> titles) {
	public static final GrandData EMPTY = new GrandData(Map.of(), Map.of());

	private static final Codec<Map<Identifier, List<UUID>>> PER_CLASS = Codec.unboundedMap(Identifier.CODEC, UUIDUtil.STRING_CODEC.listOf());
	public static final Codec<GrandData> CODEC = RecordCodecBuilder.create(i -> i.group(
		PER_CLASS.fieldOf("qualified").forGetter(GrandData::qualified),
		PER_CLASS.fieldOf("titles").forGetter(GrandData::titles)
	).apply(i, GrandData::new));

	public boolean isGrand(Identifier servantClass, UUID player) {
		return this.titles.getOrDefault(servantClass, List.of()).contains(player);
	}

	/** Whether every title of the class is held by players other than {@code player}. */
	public boolean taken(Identifier servantClass, UUID player, int grandsPerClass) {
		List<UUID> holders = this.titles.getOrDefault(servantClass, List.of());
		return !holders.contains(player) && holders.size() >= grandsPerClass;
	}

	/** The player qualified (defeated every servant of their class); titles are handed out in order. */
	public GrandData qualify(Identifier servantClass, UUID player, int grandsPerClass) {
		List<UUID> inLine = new ArrayList<>(this.qualified.getOrDefault(servantClass, List.of()));
		if (!inLine.contains(player)) {
			inLine.add(player);
		}
		return with(servantClass, inLine, this.titles.getOrDefault(servantClass, List.of())).fill(servantClass, grandsPerClass);
	}

	/**
	 * The player loses the title and must qualify again; the next in line (earliest qualified
	 * without the title) takes it. The one who lost it is never the next in line.
	 */
	public GrandData loseTitle(Identifier servantClass, UUID player, int grandsPerClass) {
		List<UUID> inLine = new ArrayList<>(this.qualified.getOrDefault(servantClass, List.of()));
		List<UUID> holders = new ArrayList<>(this.titles.getOrDefault(servantClass, List.of()));
		inLine.remove(player);
		holders.remove(player);
		return with(servantClass, inLine, holders).fill(servantClass, grandsPerClass);
	}

	private GrandData fill(Identifier servantClass, int grandsPerClass) {
		List<UUID> holders = new ArrayList<>(this.titles.getOrDefault(servantClass, List.of()));
		for (UUID candidate : this.qualified.getOrDefault(servantClass, List.of())) {
			if (holders.size() >= grandsPerClass) {
				break;
			}
			if (!holders.contains(candidate)) {
				holders.add(candidate);
			}
		}
		return with(servantClass, this.qualified.getOrDefault(servantClass, List.of()), holders);
	}

	private GrandData with(Identifier servantClass, List<UUID> inLine, List<UUID> holders) {
		Map<Identifier, List<UUID>> qualified = new HashMap<>(this.qualified);
		Map<Identifier, List<UUID>> titles = new HashMap<>(this.titles);
		qualified.put(servantClass, List.copyOf(inLine));
		titles.put(servantClass, List.copyOf(holders));
		return new GrandData(Map.copyOf(qualified), Map.copyOf(titles));
	}
}
