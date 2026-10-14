package io.github.cwzmorro.alayacore.datagen;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

final class AlayaServantClassProvider extends FabricDynamicRegistryProvider {
	// In order; the values are real max HP, attack, armor, toughness, speed %, knockback resistance, then mana regen bonus %, regen while casting.
	private static final Map<ResourceKey<ServantClass>, ServantClass> CLASSES = new LinkedHashMap<>();

	static {
		add(ServantClasses.SABER, new ServantStats(350, 36, 4, 2, 20, 0.7), 0, false);
		add(ServantClasses.ARCHER, new ServantStats(275, 24, 2, 0, 25, 0.4), 20, true);
		add(ServantClasses.LANCER, new ServantStats(325, 32, 3, 1, 30, 0.6), 0, false);
		add(ServantClasses.RIDER, new ServantStats(300, 28, 2, 1, 25, 0.5), 0, false);
		add(ServantClasses.CASTER, new ServantStats(200, 16, 1, 0, 10, 0.3), 0, false);
		add(ServantClasses.ASSASSIN, new ServantStats(225, 32, 1, 0, 30, 0.2), 0, false);
		add(ServantClasses.BERSERKER, new ServantStats(400, 40, 4, 2, 15, 1.0), 0, false);
		add(ServantClasses.RULER, new ServantStats(350, 28, 3, 1, 20, 0.6), 0, false);
		add(ServantClasses.SHIELDER, new ServantStats(375, 20, 6, 4, 15, 0.9), 0, false);
	}

	AlayaServantClassProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	private static void add(ResourceKey<ServantClass> key, ServantStats real, double manaRegenBonusPercent, boolean regenWhileCasting) {
		CLASSES.put(key, new ServantClass(CLASSES.size(), new ServantClassConfig(real, manaRegenBonusPercent, regenWhileCasting)));
	}

	static List<Identifier> ids() {
		return CLASSES.keySet().stream().map(ResourceKey::identifier).toList();
	}

	static void bootstrap(BootstrapContext<ServantClass> context) {
		CLASSES.forEach(context::register);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		// addAll filters by the datagen mod id, but these classes use the library's namespace.
		HolderLookup.RegistryLookup<ServantClass> classes = registries.lookupOrThrow(AlayaRegistries.SERVANT_CLASS_KEY);
		CLASSES.keySet().forEach(key -> entries.add(classes, key));
	}

	@Override
	public String getName() {
		return "Servant classes";
	}
}
