package io.github.cwzmorro.alayacore.rank;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code [ranks]} section of {@code config/alayacore.toml}: damage, HP and mana numbers that follow
 * from a rank. Damage and repair results are never rounded; the cost rows round their own steps ({@link RankCost}).
 *
 * @param npBaseDamage    per letter: [letter, +, ++]
 * @param skillBaseDamage per letter: [letter, +, ++], real value
 * @param repairRate      shield repair mana per HP, per letter; its + and ++ steps lie evenly between the letter and the next
 * @param skillCost       mana per skill cast, per rank step
 * @param burstCost       mana per burst NP, per rank step
 * @param beamCost        mana per second of a held beam, per rank step
 * @param shieldCastCost  mana to cast a shield, per rank step
 */
public record RankConfig(
	Map<RankLetter, List<Double>> npBaseDamage,
	Map<RankLetter, List<Double>> skillBaseDamage,
	Map<RankLetter, Double> repairRate,
	RankCost skillCost,
	RankCost burstCost,
	RankCost beamCost,
	RankCost shieldCastCost,
	double shieldHpPerNpBase,
	double beamDamagePerSecondPerNpBase,
	int beamMaxHoldSeconds,
	double beamTurnDegreesPerSecond
) {
	public static final RankConfig DEFAULTS = new RankConfig(
		perLetter(
			List.of(30.0, 38.0, 47.0),
			List.of(60.0, 75.0, 95.0),
			List.of(119.0, 143.0, 171.0),
			List.of(204.0, 244.0, 292.0),
			List.of(350.0, 415.0, 492.0),
			List.of(583.0, 692.0, 820.0)),
		perLetter(
			List.of(20.0, 22.0, 24.0),
			List.of(26.0, 29.0, 32.0),
			List.of(35.0, 38.0, 42.0),
			List.of(46.0, 50.0, 55.0),
			List.of(60.0, 70.0, 80.0),
			List.of(90.0, 107.0, 127.0)),
		perLetter(7.6, 5.1, 3.4, 2.25, 1.5, 1.0),
		new RankCost(50.0, 150.0, 200.0, 300.0, 5.0, 5.0),
		new RankCost(150.0, 900.0, 1500.0, 3400.0, 10.0, 10.0),
		new RankCost(10.0, 60.0, 130.0, 260.0, 5.0, 10.0),
		new RankCost(300.0, 700.0, 1500.0, 2500.0, 10.0, 10.0),
		1.5,
		0.2,
		15,
		30.0
	);

	@SafeVarargs
	private static <T> Map<RankLetter, T> perLetter(T... values) {
		Map<RankLetter, T> map = new EnumMap<>(RankLetter.class);
		for (RankLetter letter : RankLetter.values()) {
			map.put(letter, values[letter.ordinal()]);
		}
		return Collections.unmodifiableMap(map);
	}

	public static RankConfig read(ConfigSection s) {
		Map<RankLetter, List<Double>> np = readSteps(s.inlineSection("np_base_damage", " Damage per rank: [letter, +, ++]."),
			DEFAULTS.npBaseDamage);
		Map<RankLetter, List<Double>> skill = readSteps(s.inlineSection("skill_base_damage", null), DEFAULTS.skillBaseDamage);

		ConfigSection rates = s.inlineSection("repair_rate",
			" Mana paid per point of shield HP repaired, per letter (+ and ++ lie evenly between a letter and the next).");
		Map<RankLetter, Double> repairRate = new EnumMap<>(RankLetter.class);
		for (RankLetter letter : RankLetter.values()) {
			repairRate.put(letter, rates.number(letter.name(), null, DEFAULTS.repairRate.get(letter), 0.0, Double.MAX_VALUE));
		}

		return new RankConfig(
			np,
			skill,
			Collections.unmodifiableMap(repairRate),
			RankCost.read(s.section("skill_cost", " Mana per skill cast: the endpoints of each tier, and how its steps are rounded."),
				DEFAULTS.skillCost),
			RankCost.read(s.section("burst_cost", " Mana per burst NP."), DEFAULTS.burstCost),
			RankCost.read(s.section("beam_cost", " Mana per second of a held beam NP (also what a clash drains)."), DEFAULTS.beamCost),
			RankCost.read(s.section("shield_cast_cost", " Mana to cast a shield."), DEFAULTS.shieldCastCost),
			s.number("shield_hp_per_np_base", " Shield HP = this × NP base damage of the shield's rank.",
				DEFAULTS.shieldHpPerNpBase, 0.0, Double.MAX_VALUE),
			s.number("beam_damage_per_second_per_np_base", " Held beam NP: damage per second = this × NP base damage.",
				DEFAULTS.beamDamagePerSecondPerNpBase, 0.0, Double.MAX_VALUE),
			s.integer("beam_max_hold_seconds", null, DEFAULTS.beamMaxHoldSeconds, 0, Integer.MAX_VALUE),
			s.number("beam_turn_degrees_per_second", null, DEFAULTS.beamTurnDegreesPerSecond, 0.0, Double.MAX_VALUE)
		);
	}

	private static Map<RankLetter, List<Double>> readSteps(ConfigSection table, Map<RankLetter, List<Double>> fallback) {
		Map<RankLetter, List<Double>> map = new EnumMap<>(RankLetter.class);
		for (RankLetter letter : RankLetter.values()) {
			map.put(letter, table.numbers(letter.name(), null, fallback.get(letter), 0.0, Double.MAX_VALUE));
		}
		return Collections.unmodifiableMap(map);
	}

	public double npBaseDamage(Rank rank) {
		return this.npBaseDamage.get(rank.letter()).get(rank.pluses());
	}

	public double skillBaseDamage(Rank rank) {
		return this.skillBaseDamage.get(rank.letter()).get(rank.pluses());
	}

	public double repairRate(Rank rank) {
		RankLetter[] letters = RankLetter.values();
		int index = rank.letter().ordinal();
		double here = this.repairRate.get(rank.letter());
		boolean top = index == letters.length - 1;
		double gap = top ? here - this.repairRate.get(letters[index - 1]) : this.repairRate.get(letters[index + 1]) - here;
		return here + gap * rank.pluses() / (Rank.MAX_PLUSES + 1);
	}

	public double npDamage(Rank rank, double servantBonus) {
		return this.npBaseDamage(rank) + servantBonus;
	}

	public double skillDamage(Rank rank, double servantBonus) {
		return this.skillBaseDamage(rank) + servantBonus;
	}

	public double skillCost(Rank rank) {
		return this.skillCost.of(rank);
	}

	public double burstCost(Rank rank) {
		return this.burstCost.of(rank);
	}

	public double beamDamagePerSecond(Rank rank) {
		return this.beamDamagePerSecondPerNpBase * this.npBaseDamage(rank);
	}

	public double beamDrainPerSecond(Rank rank) {
		return this.beamCost.of(rank);
	}

	public double shieldHp(Rank rank) {
		return this.shieldHpPerNpBase * this.npBaseDamage(rank);
	}

	public double shieldCastCost(Rank rank) {
		return this.shieldCastCost.of(rank);
	}

	public double shieldRepairCost(Rank rank, double hp) {
		return hp * this.repairRate(rank);
	}
}
