package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.ability.Ability;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A servant a player can become, registered in {@link io.github.cwzmorro.alayacore.api.AlayaRegistries#SERVANT}.
 * Name and optional title come from the language file ({@code servant.<namespace>.<path>}, {@code .title});
 * true-name deeds are data.
 *
 * @param abilities everything the servant can cast; all of them come with choosing it
 * @param meleeIgnoresHitCooldown whether its melee hits land through the target's hit cooldown without starting a new one
 *                                ({@link io.github.cwzmorro.alayacore.combat.HitCooldown})
 */
public record Servant(ResourceKey<ServantClass> servantClass, List<ResourceKey<Ability>> abilities, boolean meleeIgnoresHitCooldown) {
	public Servant {
		abilities = List.copyOf(abilities);
	}

	public Servant(ResourceKey<ServantClass> servantClass, List<ResourceKey<Ability>> abilities) {
		this(servantClass, abilities, false);
	}

	public Servant meleeIgnoringHitCooldown() {
		return new Servant(this.servantClass, this.abilities, true);
	}

	public static Component name(Identifier id) {
		return Component.translatable(id.toLanguageKey("servant"));
	}

	public static Component title(Identifier id) {
		return Component.translatableWithFallback(id.toLanguageKey("servant", "title"), "");
	}
}
