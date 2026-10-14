package io.github.cwzmorro.alayacore.mana;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The {@code [mana]} section of {@code config/alayacore.toml}.
 * Percentages are kept as written in the file (3.0 = 3%).
 */
public record ManaConfig(
	double startMax,
	double maxCeiling,
	double killMaxManaMultiplier,
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
		12_000.0,
		0.1,
		3.0,
		0.25,
		20.0,
		50.0,
		Map.of(),
		0.5,
		25.0,
		500.0
	);

	public static ManaConfig read(ConfigSection s) {
		double startMax = s.number("start_max", " Max mana a new player starts with, and the max mana at which sync is 0%. Existing players keep theirs.",
			DEFAULTS.startMax, 1.0, Double.MAX_VALUE);
		double maxCeiling = s.number("max_ceiling", " Max mana stops growing here (kills, advancements, the add command).",
			DEFAULTS.maxCeiling, 0.0, Double.MAX_VALUE);
		double killMultiplier = s.number("kill_max_mana_multiplier", " Killing a mob raises max mana by its max HP × this.",
			DEFAULTS.killMaxManaMultiplier, 0.0, Double.MAX_VALUE);
		double regenPerSecond = s.number("regen_per_second", " Flat regeneration, mana per second.",
			DEFAULTS.regenPerSecond, 0.0, Double.MAX_VALUE);
		double foodPercent = s.number("food_percent_per_hunger_point", " Eating: percent of max mana per hunger point the food restores.",
			DEFAULTS.foodPercentPerHungerPoint, 0.0, Percent.WHOLE);
		double sleepPercent = s.number("sleep_percent", " Sleeping through the night: percent of max mana.",
			DEFAULTS.sleepPercent, 0.0, Percent.WHOLE);
		double respawnPercent = s.number("respawn_percent", " Current mana after respawning, percent of max mana.",
			DEFAULTS.respawnPercent, 0.0, Percent.WHOLE);

		Map<Identifier, Double> sweetFoodPercent = s.section("sweet_food",
				" Overrides the sweet_food data map's extra percent of max mana by item id.\n Cake counts per slice.")
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

		return new ManaConfig(startMax, maxCeiling, killMultiplier, regenPerSecond, foodPercent, sleepPercent, respawnPercent,
			sweetFoodPercent, xpMultiplier, minBonus, maxBonus);
	}

	public ManaData start() {
		return new ManaData(this.startMax, this.startMax);
	}

	public double killMaxManaGain(double killedMaxHealth) {
		return killedMaxHealth * this.killMaxManaMultiplier;
	}

	public double foodFraction(int nutrition, Identifier food, double dataPercent) {
		return Percent.toFraction(nutrition * this.foodPercentPerHungerPoint + this.sweetFoodPercent.getOrDefault(food, dataPercent));
	}

	public double sleepFraction() {
		return Percent.toFraction(this.sleepPercent);
	}

	public ManaData afterRespawn(ManaData mana) {
		return mana.withCurrent(mana.max() * Percent.toFraction(this.respawnPercent));
	}

	public double challengeBonus(int experienceReward) {
		return Math.clamp(experienceReward * this.challengeXpMultiplier, this.challengeMinBonus, this.challengeMaxBonus);
	}
}
