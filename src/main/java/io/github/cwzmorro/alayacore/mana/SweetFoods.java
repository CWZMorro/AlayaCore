package io.github.cwzmorro.alayacore.mana;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Data-driven sweet food percentages, with optional server config overrides. */
public final class SweetFoods {
	// Item-id map in data/<namespace>/alayacore/sweet_food/<item path>.json; cake slices use CakeFoods.SLICE.
	public static final ResourceKey<Registry<SweetFood>> KEY = ResourceKey.createRegistryKey(AlayaCore.id("sweet_food"));

	private SweetFoods() {
	}

	public static void register() {
		DynamicRegistries.register(KEY, SweetFood.CODEC);
	}

	public static double foodFraction(RegistryAccess registries, int nutrition, Identifier food) {
		return AlayaConfig.mana().foodFraction(nutrition, food, percent(registries, food));
	}

	public static double percent(RegistryAccess registries, Identifier food) {
		return registries.lookup(KEY).flatMap(registry -> registry.getOptional(food)).map(SweetFood::percent).orElse(0.0);
	}
}
