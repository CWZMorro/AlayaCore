package io.github.cwzmorro.alayacore.mana;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The {@code [mana]} section of {@code config/alayacore.toml}. Defaults are plan 7 and 13.2.
 * Percentages are kept as written in the file (3.0 = 3%).
 */
public record ManaConfig(
	double startMax,
	double regenPerSecond,
	double foodPercentPerHungerPoint,
	double sleepPercent,
	double respawnPercent,
	Map<Identifier, Double> sweetFoodPercent,
	double challengeXpMultiplier,
	double challengeMinBonus,
	double challengeMaxBonus
) {
	public static final ManaConfig DEFAULTS = new ManaConfig(
		1000.0,
		3.0,
		0.25,
		20.0,
		50.0,
		sweetFoods(),
		5.0,
		250.0,
		5000.0
	);

	private static Map<Identifier, Double> sweetFoods() {
		Map<Identifier, Double> map = new LinkedHashMap<>();
		map.put(Identifier.withDefaultNamespace("cookie"), 3.0);
		map.put(Identifier.withDefaultNamespace("cake"), 5.0);
		return Collections.unmodifiableMap(map);
	}

	public static ManaConfig read(ConfigSection s) {
		double startMax = s.number("start_max", " Max mana a new player starts with. Existing players keep theirs.",
			DEFAULTS.startMax, 0.0, Double.MAX_VALUE);
		double regenPerSecond = s.number("regen_per_second", " Flat regeneration, mana per second.",
			DEFAULTS.regenPerSecond, 0.0, Double.MAX_VALUE);
		double foodPercent = s.number("food_percent_per_hunger_point", " Eating: percent of max mana per hunger point the food restores.",
			DEFAULTS.foodPercentPerHungerPoint, 0.0, Percent.WHOLE);
		double sleepPercent = s.number("sleep_percent", " Sleeping through the night: percent of max mana.",
			DEFAULTS.sleepPercent, 0.0, Percent.WHOLE);
		double respawnPercent = s.number("respawn_percent", " Current mana after respawning, percent of max mana.",
			DEFAULTS.respawnPercent, 0.0, Percent.WHOLE);

		Map<Identifier, Double> sweetFoodPercent = s.section("sweet_food",
				" Extra percent of max mana on top of the food's own value. Any food item can be added.\n Cake counts per slice.")
			.numbersById(DEFAULTS.sweetFoodPercent, 0.0, Percent.WHOLE);

		ConfigSection challenge = s.section("challenge_advancement",
			" Challenge (purple) advancements: bonus to max mana = XP reward × xp_multiplier, between min and max.");
		double xpMultiplier = challenge.number("xp_multiplier", null, DEFAULTS.challengeXpMultiplier, 0.0, Double.MAX_VALUE);
		double minBonus = challenge.number("min", null, DEFAULTS.challengeMinBonus, 0.0, Double.MAX_VALUE);
		double maxBonus = challenge.number("max", null, DEFAULTS.challengeMaxBonus, 0.0, Double.MAX_VALUE);
		if (minBonus > maxBonus) {
			challenge.warn("min", "is above max; using the default min and max");
			minBonus = DEFAULTS.challengeMinBonus;
			maxBonus = DEFAULTS.challengeMaxBonus;
		}

		return new ManaConfig(startMax, regenPerSecond, foodPercent, sleepPercent, respawnPercent,
			sweetFoodPercent, xpMultiplier, minBonus, maxBonus);
	}

	/** Mana of a new player: full. */
	public ManaData start() {
		return new ManaData(this.startMax, this.startMax);
	}

	/** Fraction of max regained from eating: the food's hunger points plus its sweet bonus, if any. */
	public double foodFraction(int nutrition, Identifier food) {
		return Percent.toFraction(nutrition * this.foodPercentPerHungerPoint + this.sweetFoodPercent.getOrDefault(food, 0.0));
	}

	public double sleepFraction() {
		return Percent.toFraction(this.sleepPercent);
	}

	/** Max is kept; current is set to the respawn percentage of it. */
	public ManaData afterRespawn(ManaData mana) {
		return mana.withCurrent(mana.max() * Percent.toFraction(this.respawnPercent));
	}

	/** Max mana for completing a challenge advancement with this XP reward (0 if it has none). */
	public double challengeBonus(int experienceReward) {
		return Math.clamp(experienceReward * this.challengeXpMultiplier, this.challengeMinBonus, this.challengeMaxBonus);
	}
}
