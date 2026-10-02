package io.github.cwzmorro.alayacore.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cwzmorro.alayacore.ability.AbilityConfig;
import io.github.cwzmorro.alayacore.clash.ClashConfig;
import io.github.cwzmorro.alayacore.divinity.DivinityConfig;
import io.github.cwzmorro.alayacore.mana.ManaConfig;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankConfig;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantConfig;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import io.github.cwzmorro.alayacore.shield.ShieldConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlayaConfigTest {
	private static final Identifier ARCHER = Identifier.fromNamespaceAndPath("alayacore", "archer");
	private static final ServantClassConfig ARCHER_DEFAULTS = new ServantClassConfig(new ServantStats(275, 24, 2, 0, 25, 0.4), 20.0, true);
	private static final Map<Identifier, ServantClassConfig> CLASSES = Map.of(ARCHER, ARCHER_DEFAULTS);
	private static final Identifier GATES = Identifier.fromNamespaceAndPath("test", "gates");
	private static final Map<Identifier, List<Double>> ABILITIES = Map.of(GATES, List.of(0.0, 2.5));
	/** A content mod's table (registered once: the registry is the game's). */
	private static final int VOLLEY = 25;
	private static final ConfigTable<Integer> CONTENT = AlayaConfig.register("testmod", " A content mod's settings.", VOLLEY,
		s -> s.integer("volley", " Gates a volley.", VOLLEY, 1, Integer.MAX_VALUE));

	@TempDir
	Path dir;

	private Path file() {
		return this.dir.resolve(AlayaConfig.FILE_NAME);
	}

	private String text() throws IOException {
		return Files.readString(this.file(), StandardCharsets.UTF_8);
	}

	private void edit(String from, String to) throws IOException {
		String text = this.text();
		assertTrue(text.contains(from), "file contains: " + from);
		Files.writeString(this.file(), text.replace(from, to), StandardCharsets.UTF_8);
	}

	@AfterEach
	void backToDefaults() throws IOException {
		Files.deleteIfExists(this.file());
		AlayaConfig.load(this.file(), CLASSES, ABILITIES);
	}

	@Test
	void missingFileIsWrittenWithDefaultsAndReadsBackTheSame() throws IOException {
		assertEquals(List.of(), AlayaConfig.load(this.file(), CLASSES, ABILITIES));
		assertEquals(ManaConfig.DEFAULTS, AlayaConfig.mana());
		assertEquals(RankConfig.DEFAULTS, AlayaConfig.ranks());
		assertEquals(new ServantConfig(false, CLASSES), AlayaConfig.servants());
		String written = this.text();
		assertTrue(written.contains("allow_duplicates = false"), written);
		assertTrue(written.contains("[classes.\"alayacore:archer\"]"), written);
		assertTrue(written.contains("mana_regen_bonus_percent = 20.0"), written);
		assertTrue(written.contains("regen_while_casting = true"), written);
		assertTrue(written.contains("hold_threshold_seconds = 0.25"), written);
		assertTrue(written.contains("\"test:gates\" = [0.0, 2.5]"), written);
		assertEquals(new AbilityConfig(0.25, ABILITIES), AlayaConfig.abilities());
		assertEquals(ClashConfig.DEFAULTS, AlayaConfig.clash());
		assertEquals(DivinityConfig.DEFAULTS, AlayaConfig.divinity());
		assertEquals(ShieldConfig.DEFAULTS, AlayaConfig.shields());
		assertTrue(written.contains("tap_seconds = 300"), written);
		assertTrue(written.contains("hold_upkeep_per_second = 5.0"), written);
		assertTrue(written.contains("loser_hit_percent = 90.0"), written);
		assertTrue(written.contains("close_lower_drain_multiplier = 1.5"), written);
		assertTrue(written.contains("regen_per_second = 3.0"), written);
		assertTrue(written.contains("\"minecraft:cookie\" = 3.0"), written);
		assertTrue(written.contains("# Flat regeneration, mana per second."), written);

		assertEquals(List.of(), AlayaConfig.load(this.file(), CLASSES, ABILITIES));
		assertEquals(ManaConfig.DEFAULTS, AlayaConfig.mana());
		assertEquals(RankConfig.DEFAULTS, AlayaConfig.ranks());
		assertEquals(written, this.text());
	}

	@Test
	void editedValuesAreUsed() throws IOException {
		AlayaConfig.load(this.file(), CLASSES, ABILITIES);
		this.edit("regen_per_second = 3.0", "regen_per_second = 5.0");
		this.edit("\"minecraft:cookie\" = 3.0", "\"minecraft:cookie\" = 3.0\n\"minecraft:cooked_beef\" = 2.0");
		this.edit("A = [350.0, 415.0, 492.0]", "A = [360.0, 415.0, 492.0]");
		this.edit("allow_duplicates = false", "allow_duplicates = true");
		this.edit("max_hp = 275.0", "max_hp = 300.0");
		this.edit("[divinity]", "[divinity]\n\"minecraft:warden\" = 2.0");

		assertEquals(List.of(), AlayaConfig.load(this.file(), CLASSES, ABILITIES));
		assertEquals(5.0, AlayaConfig.mana().regenPerSecond());
		assertEquals(2.0, AlayaConfig.mana().sweetFoodPercent().get(Identifier.withDefaultNamespace("cooked_beef")));
		assertEquals(360.0, AlayaConfig.ranks().npBaseDamage(Rank.parse("A")));
		assertTrue(AlayaConfig.servants().allowDuplicates());
		assertEquals(300.0, AlayaConfig.servants().classes().get(ARCHER).real().maxHp());
		assertEquals(Optional.of(2.0), AlayaConfig.divinity().divinity(Identifier.withDefaultNamespace("warden")));
		assertTrue(this.text().contains("regen_per_second = 5.0"));
	}

	@Test
	void invalidValueFallsBackToDefaultWithAWarning() throws IOException {
		AlayaConfig.load(this.file(), CLASSES, ABILITIES);
		this.edit("sleep_percent = 20.0", "sleep_percent = 250.0");
		this.edit("\"minecraft:cookie\" = 3.0", "\"minecraft:cookie\" = \"lots\"");

		List<String> warnings = AlayaConfig.load(this.file(), CLASSES, ABILITIES);
		assertEquals(2, warnings.size(), warnings.toString());
		assertEquals(ManaConfig.DEFAULTS.sleepPercent(), AlayaConfig.mana().sleepPercent());
		assertEquals(null, AlayaConfig.mana().sweetFoodPercent().get(Identifier.withDefaultNamespace("cookie")));
	}

	@Test
	void minAboveMaxUsesDefaults() throws IOException {
		AlayaConfig.load(this.file(), CLASSES, ABILITIES);
		this.edit("min = 250.0", "min = 9000.0");
		assertEquals(1, AlayaConfig.load(this.file(), CLASSES, ABILITIES).size());
		assertEquals(ManaConfig.DEFAULTS.challengeMinBonus(), AlayaConfig.mana().challengeMinBonus());
	}

	@Test
	void missingKeyIsAddedBack() throws IOException {
		AlayaConfig.load(this.file(), CLASSES, ABILITIES);
		this.edit("respawn_percent = 50.0\n", "");
		assertEquals(List.of(), AlayaConfig.load(this.file(), CLASSES, ABILITIES));
		assertTrue(this.text().contains("respawn_percent = 50.0"));
	}

	@Test
	void contentModTablesAreWrittenAndRead() throws IOException {
		assertEquals(List.of(), AlayaConfig.load(this.file(), CLASSES, ABILITIES));
		assertTrue(this.text().contains("[testmod]"), this.text());
		this.edit("volley = 25", "volley = 40");
		assertEquals(List.of(), AlayaConfig.load(this.file(), CLASSES, ABILITIES));
		assertEquals(40, CONTENT.get());
	}

	@Test
	void aTakenTableIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> AlayaConfig.register("mana", null, 0, s -> 0));
		assertThrows(IllegalArgumentException.class, () -> AlayaConfig.register("testmod", null, 0, s -> 0));
	}

	@Test
	void brokenFileIsLeftAloneAndValuesKept() throws IOException {
		AlayaConfig.load(this.file(), CLASSES, ABILITIES);
		this.edit("regen_per_second = 3.0", "regen_per_second = 4.0");
		AlayaConfig.load(this.file(), CLASSES, ABILITIES);
		Files.writeString(this.file(), "this is [not toml", StandardCharsets.UTF_8);

		assertEquals(1, AlayaConfig.load(this.file(), CLASSES, ABILITIES).size());
		assertEquals(4.0, AlayaConfig.mana().regenPerSecond());
		assertEquals("this is [not toml", this.text());
	}
}
