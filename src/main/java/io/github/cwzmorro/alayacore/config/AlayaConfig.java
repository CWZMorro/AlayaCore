package io.github.cwzmorro.alayacore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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
 * {@code config/alayacore.toml}: one file for the whole game or server, read at server start and by
 * {@code /alayacore config reload}. Until it is read, the plan.md defaults are used. Content mods add their own tables
 * with {@link #register}.
 */
public final class AlayaConfig {
	public static final String FILE_NAME = AlayaCore.MOD_ID + ".toml";
	private static final String HEADER = """
		# Alaya Core settings. Read at server start; /alayacore config reload re-reads it (op level 2).
		# A missing or invalid value falls back to its default, with a warning in the log.

		""";

	/** Alaya Core's own tables, which no content mod may take. */
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
	/** Content mods' tables, after Alaya Core's in the file, in the order they were registered. */
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

	/**
	 * Adds a content mod's table to the file, {@code [key]} (its mod id), read by {@code reader} at every load; call it while the
	 * mod initializes.
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

	/** Loads {@code config/alayacore.toml} with every registered servant class and ability. */
	public static List<String> load() {
		Map<Identifier, ServantClassConfig> classDefaults = new LinkedHashMap<>();
		for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
			classDefaults.put(AlayaRegistries.SERVANT_CLASS.getKey(servantClass), servantClass.defaults());
		}
		Map<Identifier, List<Double>> cooldownDefaults = new LinkedHashMap<>();
		for (Ability ability : AlayaRegistries.ABILITY) {
			cooldownDefaults.put(AlayaRegistries.ABILITY.getKey(ability), ability.defaultCooldownSeconds());
		}
		return load(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME), classDefaults, cooldownDefaults);
	}

	/**
	 * Reads the file, uses its values, and writes it back with any missing keys and all comments.
	 * A file that cannot be parsed is left untouched and the current values are kept.
	 *
	 * @param classDefaults    every servant class with its built-in values
	 * @param cooldownDefaults every ability with its built-in cooldowns
	 * @return the problems found, empty if none
	 */
	public static List<String> load(Path file, Map<Identifier, ServantClassConfig> classDefaults, Map<Identifier, List<Double>> cooldownDefaults) {
		List<String> warnings = new ArrayList<>();
		CommentedConfig input = null;
		String existing = null;
		if (Files.exists(file)) {
			try {
				existing = Files.readString(file, StandardCharsets.UTF_8);
				input = TomlFormat.instance().createParser().parse(new StringReader(existing));
			} catch (IOException | ParsingException e) {
				warnings.add("cannot read " + file + ", keeping the current values: " + e.getMessage());
				return report(warnings);
			}
		}

		CommentedConfig output = TomlFormat.newConfig(LinkedHashMap::new);
		Set<UnmodifiableConfig> inlineTables = Collections.newSetFromMap(new IdentityHashMap<>());
		ConfigSection root = new ConfigSection(input, output, "", warnings, inlineTables);
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

		String text = HEADER + toToml(output, inlineTables);
		if (!text.equals(existing)) {
			write(file, text, warnings);
		}
		return report(warnings);
	}

	private static String toToml(CommentedConfig config, Set<UnmodifiableConfig> inlineTables) {
		TomlWriter writer = TomlFormat.instance().createWriter();
		writer.setWriteTableInlinePredicate(inlineTables::contains);
		StringWriter out = new StringWriter();
		writer.write(config, out);
		return out.toString();
	}

	private static void write(Path file, String text, List<String> warnings) {
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temp, text, StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			warnings.add("cannot write " + file + ": " + e.getMessage());
		}
	}

	private static List<String> report(List<String> warnings) {
		warnings.forEach(warning -> AlayaCore.LOGGER.warn("Config: {}", warning));
		return List.copyOf(warnings);
	}
}
