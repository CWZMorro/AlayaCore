package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/** Alaya Core's classes: the seven standard ones in Fate's usual order, then Ruler and Shielder. Values: plan 7 and 13.7. */
public final class ServantClasses {
	// Real values: max HP, attack, armor, toughness, speed %, knockback resistance (plan 13.7); mana regen bonus %, regen while casting (plan 7).
	public static final ResourceKey<ServantClass> SABER = register("saber", new ServantStats(350, 36, 4, 2, 20, 0.7), 0, false);
	public static final ResourceKey<ServantClass> ARCHER = register("archer", new ServantStats(275, 24, 2, 0, 25, 0.4), 20, true);
	public static final ResourceKey<ServantClass> LANCER = register("lancer", new ServantStats(325, 32, 3, 1, 30, 0.6), 0, false);
	public static final ResourceKey<ServantClass> RIDER = register("rider", new ServantStats(300, 28, 2, 1, 25, 0.5), 0, false);
	public static final ResourceKey<ServantClass> CASTER = register("caster", new ServantStats(200, 16, 1, 0, 10, 0.3), 0, false);
	public static final ResourceKey<ServantClass> ASSASSIN = register("assassin", new ServantStats(225, 32, 1, 0, 30, 0.2), 0, false);
	public static final ResourceKey<ServantClass> BERSERKER = register("berserker", new ServantStats(400, 40, 4, 2, 15, 1.0), 0, false);
	public static final ResourceKey<ServantClass> RULER = register("ruler", new ServantStats(350, 28, 3, 1, 20, 0.6), 0, false);
	public static final ResourceKey<ServantClass> SHIELDER = register("shielder", new ServantStats(375, 20, 6, 4, 15, 0.9), 0, false);

	private ServantClasses() {
	}

	private static ResourceKey<ServantClass> register(String name, ServantStats real, double manaRegenBonusPercent, boolean regenWhileCasting) {
		ResourceKey<ServantClass> key = ResourceKey.create(AlayaRegistries.SERVANT_CLASS_KEY, AlayaCore.id(name));
		Registry.register(AlayaRegistries.SERVANT_CLASS, key, new ServantClass(new ServantClassConfig(real, manaRegenBonusPercent, regenWhileCasting)));
		return key;
	}

	/** Loads this class, which registers the classes. */
	public static void init() {
	}
}
