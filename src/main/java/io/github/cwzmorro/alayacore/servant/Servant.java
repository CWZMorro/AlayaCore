package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.divinity.DivinityRules;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A servant a player can become. Content mods register theirs in
 * {@link io.github.cwzmorro.alayacore.api.AlayaRegistries#SERVANT}.
 *
 * @param abilities everything the servant can cast; all of them come with choosing it
 * @param divinity  its divinity at 100% sync, 0 to 10; 0 for none
 */
public record Servant(ResourceKey<ServantClass> servantClass, List<ResourceKey<Ability>> abilities, int divinity) {
	public Servant {
		abilities = List.copyOf(abilities);
		if (divinity < 0 || divinity > DivinityRules.MAX) {
			throw new IllegalArgumentException("divinity must be 0.." + DivinityRules.MAX + ", got " + divinity);
		}
	}

	public Servant(ResourceKey<ServantClass> servantClass, List<ResourceKey<Ability>> abilities) {
		this(servantClass, abilities, 0);
	}

	public static Component name(Identifier id) {
		return Component.translatable(id.toLanguageKey("servant"));
	}

	public static Component title(Identifier id) {
		return Component.translatableWithFallback(id.toLanguageKey("servant", "title"), "");
	}
}
