package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import java.util.List;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

// Separate servants keep tests from claiming each other's selection.
public final class TestServants implements ModInitializer, DedicatedServerModInitializer {
	public static final Identifier SOLO = id("solo");
	public static final Identifier CONTESTED = id("contested");
	public static final Identifier OLD = id("old");
	public static final Identifier NEW = id("new");
	public static final Identifier IGNORED = id("ignored");
	public static final Identifier FORCED = id("forced");
	public static final Identifier ANNOUNCED = id("announced");
	public static final Identifier ARCHER = id("archer");
	public static final Identifier ARCHER_TWO = id("archer_two");
	public static final Identifier CASTER = id("caster");
	public static final Identifier DIVINE = id("divine");
	static final int DIVINE_MAX = 5;
	public static final Identifier DIVINE_TWO = id("divine_two");
	public static final Identifier RANKED = id("ranked");
	public static final Identifier LEGEND = id("legend");
	// STARTER, RELENTLESS, DATA_CLASS_SERVANT and BOWMAN are for server game tests only, so the client tests' selection screen is unchanged.
	public static final Identifier STARTER = id("starter");
	public static final Identifier RELENTLESS = id("relentless");
	// Data-only classes force the contents list onto a second page.
	public static final ResourceKey<ServantClass> DATA_CLASS = ResourceKey.create(AlayaRegistries.SERVANT_CLASS_KEY, id("test_class_8"));
	public static final Identifier DATA_CLASS_SERVANT = id("data_class_servant");

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("alayacore-gametest", path);
	}

	@Override
	public void onInitialize() {
		TestAbilities.register();
		TestFoods.register();
		TestShields.init();
		TestMarbles.register();
		TestDeeds.init();
		for (Identifier id : new Identifier[]{SOLO, CONTESTED, OLD, NEW, IGNORED, FORCED, ANNOUNCED}) {
			Registry.register(AlayaRegistries.SERVANT, id, new Servant(ServantClasses.SABER, List.of()));
		}
		Registry.register(AlayaRegistries.SERVANT, DIVINE, new Servant(ServantClasses.SABER, List.of()));
		Registry.register(AlayaRegistries.SERVANT, DIVINE_TWO, new Servant(ServantClasses.SABER, List.of()));
		Registry.register(AlayaRegistries.SERVANT, ARCHER, new Servant(ServantClasses.ARCHER, List.of(TestAbilities.RECORDER)));
		Registry.register(AlayaRegistries.SERVANT, RANKED, new Servant(ServantClasses.RIDER,
			List.of(TestAbilities.RECORDER, TestAbilities.RANGED, TestAbilities.UNKNOWN, TestAbilities.PASSIVE)));
		Registry.register(AlayaRegistries.SERVANT, ARCHER_TWO, new Servant(ServantClasses.ARCHER, List.of()));
		Registry.register(AlayaRegistries.SERVANT, CASTER, new Servant(ServantClasses.SABER, List.of(TestAbilities.RECORDER,
			TestAbilities.EXCLUSIVE, TestAbilities.NOBLE_PHANTASM, TestAbilities.COOLDOWN, TestAbilities.COSTLY, TestAbilities.PICKY,
			TestAbilities.CONFIGURABLE, TestAbilities.PASSIVE)));
		Registry.register(AlayaRegistries.SERVANT, LEGEND, new Servant(ServantClasses.SABER, List.of()));
	}

	@Override
	public void onInitializeServer() {
		Registry.register(AlayaRegistries.SERVANT, STARTER, new Servant(ServantClasses.SABER, List.of()));
		Registry.register(AlayaRegistries.SERVANT, DATA_CLASS_SERVANT, new Servant(DATA_CLASS, List.of()));
		Registry.register(AlayaRegistries.SERVANT, RELENTLESS, new Servant(ServantClasses.SABER, List.of()).meleeIgnoringHitCooldown());
	}
}
