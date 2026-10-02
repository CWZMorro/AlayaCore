package io.github.cwzmorro.alayacore.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * A servant's legend (plan 13.3), from {@code data/<namespace>/alayacore/true_name/<servant>.json}; the
 * file's id is the servant's id. Library users write their own.
 *
 * @param branch     the servant's advancement branch, granted when a player becomes that servant
 * @param deeds      done in this order, each counting only once the one before it is done; all of them
 *                   before the true name can awaken
 * @param awakening  the true name advancement, granted by defeating a boss once the deeds are done
 */
public record TrueNameDeeds(Identifier branch, List<Deed> deeds, Identifier awakening) {
	/**
	 * One deed: an advancement granted when its condition happens. Exactly one of {@code kill} (an
	 * entity type id) and {@code winRaid} is set.
	 */
	public record Deed(Identifier advancement, Optional<Identifier> kill, boolean winRaid) {
		public static final Codec<Deed> CODEC = RecordCodecBuilder.<Deed>create(i -> i.group(
			Identifier.CODEC.fieldOf("advancement").forGetter(Deed::advancement),
			Identifier.CODEC.optionalFieldOf("kill").forGetter(Deed::kill),
			Codec.BOOL.optionalFieldOf("win_raid", false).forGetter(Deed::winRaid)
		).apply(i, Deed::new)).validate(deed -> deed.kill.isPresent() != deed.winRaid
			? DataResult.success(deed)
			: DataResult.error(() -> "deed " + deed.advancement + " needs exactly one of \"kill\" and \"win_raid\""));
	}

	public static final Codec<TrueNameDeeds> CODEC = RecordCodecBuilder.create(i -> i.group(
		Identifier.CODEC.fieldOf("branch").forGetter(TrueNameDeeds::branch),
		Deed.CODEC.listOf().fieldOf("deeds").forGetter(TrueNameDeeds::deeds),
		Identifier.CODEC.fieldOf("awakening").forGetter(TrueNameDeeds::awakening)
	).apply(i, TrueNameDeeds::new));
}
