package io.github.cwzmorro.alayacore.config;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityConfig;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.clash.ClashConfig;
import io.github.cwzmorro.alayacore.combat.CombatConfig;
import io.github.cwzmorro.alayacore.divinity.DivinityConfig;
import io.github.cwzmorro.alayacore.mana.ManaConfig;
import io.github.cwzmorro.alayacore.progression.ProgressionConfig;
import io.github.cwzmorro.alayacore.rank.RankConfig;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantConfig;
import io.github.cwzmorro.alayacore.shield.ShieldConfig;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Loads server settings from {@code config/alayacore.toml} at startup and on config reload.
 * Uses defaults until loaded; content mods add tables with {@link #register}.
 */
public final class AlayaConfig {
	public static final String FILE_NAME = AlayaCore.MOD_ID + ".toml";
	private static final String HEADER = """
		# Alaya Core settings. Read at server start; /alayacore config reload re-reads it (op level 2).
		# A missing or invalid value falls back to its default, with a warning in the log.

		""";

	private static final String MANA = "mana";
	private static final String RANKS = "ranks";
	private static final String SERVANTS = "servants";
	private static final String CLASSES = "classes";
	private static final String PROGRESSION = "progression";
	private static final String COMBAT = "combat";
	private static final String ABILITIES = "abilities";
	private static final String CLASH = "clash";
	private static final String DIVINITY = "divinity";
	private static final String SHIELDS = "shields";
	private static final Set<String> OWN_TABLES = Set.of(MANA, RANKS, SERVANTS, CLASSES, PROGRESSION, COMBAT, ABILITIES, CLASH, DIVINITY, SHIELDS);

	private record Values(ManaConfig mana, RankConfig ranks, ServantConfig servants, ProgressionConfig progression, CombatConfig combat,
		AbilityConfig abilities, ClashConfig clash, DivinityConfig divinity, ShieldConfig shields) {
	}

	private static volatile Values values = new Values(ManaConfig.DEFAULTS, RankConfig.DEFAULTS, ServantConfig.DEFAULTS,
		ProgressionConfig.DEFAULTS, CombatConfig.DEFAULTS, AbilityConfig.DEFAULTS, ClashConfig.DEFAULTS,
		DivinityConfig.DEFAULTS, ShieldConfig.DEFAULTS);
	private static final List<ConfigTable<?>> TABLES = new CopyOnWriteArrayList<>();

	private AlayaConfig() {
	}

	public static ManaConfig mana() {
		return values.mana();
	}

	public static RankConfig ranks() {
		return values.ranks();
	}

	public static ServantConfig servants() {
		return values.servants();
	}

	public static ProgressionConfig progression() {
		return values.progression();
	}

	public static CombatConfig combat() {
		return values.combat();
	}

	public static AbilityConfig abilities() {
		return values.abilities();
	}

	public static ClashConfig clash() {
		return values.clash();
	}

	public static DivinityConfig divinity() {
		return values.divinity();
	}

	public static ShieldConfig shields() {
		return values.shields();
	}

	/** Applies synced shield settings on the client for ability descriptions. */
	public static void useServerShields(ShieldConfig shields) {
		Values current = values;
		values = new Values(current.mana(), current.ranks(), current.servants(), current.progression(), current.combat(),
			current.abilities(), current.clash(), current.divinity(), shields);
	}

	/**
	 * Registers a content mod's table for each config load. Call during mod initialization.
	 * The key is normally the mod id; a null comment omits the table comment.
	 *
	 * @throws IllegalArgumentException if Alaya Core or another mod already has a table by that name
	 */
	public static <T> ConfigTable<T> register(String key, @Nullable String comment, T defaults, Function<ConfigSection, T> reader) {
		if (OWN_TABLES.contains(key) || TABLES.stream().anyMatch(table -> table.key().equals(key))) {
			throw new IllegalArgumentException("config table [" + key + "] is taken");
		}
		ConfigTable<T> table = new ConfigTable<>(key, comment, defaults, reader);
		TABLES.add(table);
		return table;
	}

	public static List<String> load() {
		Map<Identifier, ServantClassConfig> classDefaults = new LinkedHashMap<>();
		for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
			classDefaults.put(AlayaRegistries.SERVANT_CLASS.getKey(servantClass), servantClass.defaults());
		}
		Map<Identifier, List<Double>> cooldownDefaults = new LinkedHashMap<>();
		for (Ability ability : AlayaRegistries.ABILITY) {
			if (!ability.passive()) {
				cooldownDefaults.put(AlayaRegistries.ABILITY.getKey(ability), ability.defaultCooldownSeconds());
			}
		}
		return load(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME), classDefaults, cooldownDefaults);
	}

	/**
	 * Reads the file, uses its values, and writes it back with any missing keys and all comments.
	 * A file that cannot be parsed is left untouched and the current values are kept.
	 *
	 * @param classDefaults    every servant class with its built-in values
	 * @param cooldownDefaults every ability with a cooldown, with its built-in cooldowns
	 * @return the problems found, empty if none
	 */
	public static List<String> load(Path file, Map<Identifier, ServantClassConfig> classDefaults, Map<Identifier, List<Double>> cooldownDefaults) {
		return ConfigFile.sync(file, HEADER, true, root -> {
			values = new Values(
				ManaConfig.read(root.section(MANA, null)),
				RankConfig.read(root.section(RANKS, null)),
				ServantConfig.read(root.section(SERVANTS, null), root.section(CLASSES, ServantConfig.CLASSES_COMMENT), classDefaults),
				ProgressionConfig.read(root.section(PROGRESSION, null)),
				CombatConfig.read(root.section(COMBAT, null)),
				AbilityConfig.read(root.section(ABILITIES, null), cooldownDefaults),
				ClashConfig.read(root.section(CLASH, null)),
				DivinityConfig.read(root.section(DIVINITY, DivinityConfig.COMMENT)),
				ShieldConfig.read(root.section(SHIELDS, null))
			);
			TABLES.forEach(table -> table.read(root));
		});
	}
}
