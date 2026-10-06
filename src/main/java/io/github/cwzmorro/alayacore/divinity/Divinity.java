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
 * Calculates divinity from servant synchronization or entity-type defaults without storing it on the entity.
 */
public final class Divinity {
	public static final ResourceKey<Registry<DivinityDefault>> DEFAULTS_KEY = ResourceKey.createRegistryKey(AlayaCore.id("divinity"));

	private Divinity() {
	}

	public static void register() {
		DynamicRegistries.register(DEFAULTS_KEY, DivinityDefault.CODEC);
	}

	/**
	 * Returns server-side divinity from servant progress, or config then data defaults for other entities.
	 * Missing data yields zero.
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
