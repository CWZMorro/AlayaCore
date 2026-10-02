package io.github.cwzmorro.alayacore.divinity;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.servant.Servant;
import java.util.Optional;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;

/**
 * Divinity, 0 to 10 (plan 10): its own stat, not an effect. Worked out when asked, never stored: a servant's
 * from its servant and sync, anything else from its entity type.
 */
public final class Divinity {
	/** The data files' defaults per entity type ({@link DivinityDefault}); read on the server. */
	public static final ResourceKey<Registry<DivinityDefault>> DEFAULTS_KEY = ResourceKey.createRegistryKey(AlayaCore.id("divinity"));

	private Divinity() {
	}

	public static void register() {
		DynamicRegistries.register(DEFAULTS_KEY, DivinityDefault.CODEC);
	}

	/**
	 * The entity's divinity. A servant: from its servant's max and its sync ({@link DivinityRules#servantDivinity}).
	 * Anything else: the {@code [divinity]} config table, else the data files, else 0. Server side.
	 */
	public static double of(LivingEntity entity) {
		Optional<Servant> servant = AlayaServants.servant(entity);
		if (servant.isPresent()) {
			return DivinityRules.servantDivinity(servant.get().divinity(), Progression.sync(entity));
		}
		Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
		return AlayaConfig.divinity().divinity(type).orElseGet(() -> entity.level().registryAccess().lookup(DEFAULTS_KEY)
			.flatMap(defaults -> defaults.getOptional(type))
			.map(DivinityDefault::divinity)
			.orElse(0.0));
	}
}
