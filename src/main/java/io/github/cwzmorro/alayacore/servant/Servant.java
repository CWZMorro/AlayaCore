package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.divinity.DivinityRules;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A servant a player can become (e.g. Gilgamesh). Content mods register theirs in
 * {@link io.github.cwzmorro.alayacore.api.AlayaRegistries#SERVANT}.
 *
 * <p>Name and description come from the language file: {@code servant.<namespace>.<path>} and
 * {@code servant.<namespace>.<path>.description}. True-name deeds are data (plan 13.3).
 *
 * @param abilities everything the servant can cast; all of them come with choosing it (plan 3)
 * @param divinity  its divinity at 100% sync, 0 to 10; 0 for none (plan 10)
 */
public record Servant(ResourceKey<ServantClass> servantClass, List<ResourceKey<Ability>> abilities, int divinity) {
	public Servant {
		abilities = List.copyOf(abilities);
		if (divinity < 0 || divinity > DivinityRules.MAX) {
			throw new IllegalArgumentException("divinity must be 0.." + DivinityRules.MAX + ", got " + divinity);
		}
	}

	/** A servant without divinity. */
	public Servant(ResourceKey<ServantClass> servantClass, List<ResourceKey<Ability>> abilities) {
		this(servantClass, abilities, 0);
	}

	public static Component name(Identifier id) {
		return Component.translatable(id.toLanguageKey("servant"));
	}

	public static Component description(Identifier id) {
		return Component.translatable(id.toLanguageKey("servant", "description"));
	}
}
