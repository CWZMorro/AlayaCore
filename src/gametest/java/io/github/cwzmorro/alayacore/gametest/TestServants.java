package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

// Separate servants keep tests from claiming each other's selection.
public final class TestServants implements ModInitializer {
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
	public static final Identifier RANKED = id("ranked");
	// Classes without servants force the contents list onto a second page.
	private static final List<String> EXTRA_CLASSES = List.of("avenger", "alter_ego", "foreigner", "moon_cancer", "beast", "pretender",
		"test_class_1", "test_class_2", "test_class_3", "test_class_4", "test_class_5", "test_class_6", "test_class_7", "test_class_8");
	private static final ServantClassConfig EXTRA_CLASS_VALUES = new ServantClassConfig(new ServantStats(300, 28, 2, 1, 20, 0.5), 0, false);

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("alayacore-gametest", path);
	}

	@Override
	public void onInitialize() {
		TestAbilities.register();
		TestShields.init();
		for (Identifier id : new Identifier[]{SOLO, CONTESTED, OLD, NEW, IGNORED, FORCED, ANNOUNCED}) {
			Registry.register(AlayaRegistries.SERVANT, id, new Servant(ServantClasses.SABER, List.of()));
		}
		Registry.register(AlayaRegistries.SERVANT, DIVINE, new Servant(ServantClasses.SABER, List.of(), DIVINE_MAX));
		Registry.register(AlayaRegistries.SERVANT, ARCHER, new Servant(ServantClasses.ARCHER, List.of(TestAbilities.RECORDER)));
		Registry.register(AlayaRegistries.SERVANT, RANKED, new Servant(ServantClasses.RIDER,
			List.of(TestAbilities.RECORDER, TestAbilities.RANGED, TestAbilities.UNKNOWN)));
		Registry.register(AlayaRegistries.SERVANT, ARCHER_TWO, new Servant(ServantClasses.ARCHER, List.of()));
		Registry.register(AlayaRegistries.SERVANT, CASTER, new Servant(ServantClasses.SABER, List.of(TestAbilities.RECORDER,
			TestAbilities.EXCLUSIVE, TestAbilities.NOBLE_PHANTASM, TestAbilities.COOLDOWN, TestAbilities.COSTLY, TestAbilities.PICKY,
			TestAbilities.CONFIGURABLE)));
		// After the servants above, which load Alaya Core's own classes, so these come after them.
		for (String name : EXTRA_CLASSES) {
			Registry.register(AlayaRegistries.SERVANT_CLASS, id(name), new ServantClass(EXTRA_CLASS_VALUES));
		}
	}
}
