package io.github.cwzmorro.alayacore.rank;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code [ranks]} section of {@code config/alayacore.toml}: damage, HP and mana numbers that follow
 * from a rank. Defaults are plan 13.7. Results are exact and never rounded (plan 9).
 *
 * @param npBaseDamage   per letter: [letter, +, ++]
 * @param skillBaseDamage per letter: [letter, +, ++], real value
 * @param manaRate       mana per point of damage or HP; B, B+ and B++ all use B's rate
 */
public record RankConfig(
	Map<RankLetter, List<Double>> npBaseDamage,
	Map<RankLetter, List<Double>> skillBaseDamage,
	Map<RankLetter, Double> manaRate,
	double shieldHpPerNpBase,
	double beamDamagePerSecondPerNpBase,
	int beamMaxHoldSeconds,
	double beamTurnDegreesPerSecond,
	double skillDemiMinDamage
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
		1.5,
		0.2,
		15,
		30.0,
		15.0
	);

	/** Values given in letter order, E first. */
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

		ConfigSection rates = s.inlineSection("mana_rate", " Mana paid per point of damage or shield HP, per letter (B+ uses B).");
		Map<RankLetter, Double> manaRate = new EnumMap<>(RankLetter.class);
		for (RankLetter letter : RankLetter.values()) {
			manaRate.put(letter, rates.number(letter.name(), null, DEFAULTS.manaRate.get(letter), 0.0, Double.MAX_VALUE));
		}

		return new RankConfig(
			np,
			skill,
			Collections.unmodifiableMap(manaRate),
			s.number("shield_hp_per_np_base", " Shield HP = this × NP base damage of the shield's rank.",
				DEFAULTS.shieldHpPerNpBase, 0.0, Double.MAX_VALUE),
			s.number("beam_damage_per_second_per_np_base", " Held beam NP: damage per second = this × NP base damage.",
				DEFAULTS.beamDamagePerSecondPerNpBase, 0.0, Double.MAX_VALUE),
			s.integer("beam_max_hold_seconds", null, DEFAULTS.beamMaxHoldSeconds, 0, Integer.MAX_VALUE),
			s.number("beam_turn_degrees_per_second", null, DEFAULTS.beamTurnDegreesPerSecond, 0.0, Double.MAX_VALUE),
			s.number("skill_demi_min_damage", " Lowest demi value of any damaging skill.",
				DEFAULTS.skillDemiMinDamage, 0.0, Double.MAX_VALUE)
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

	public double manaRate(Rank rank) {
		return this.manaRate.get(rank.letter());
	}

	/** Damage of a noble phantasm: the rank's base plus the servant's own bonus. */
	public double npDamage(Rank rank, double servantBonus) {
		return this.npBaseDamage(rank) + servantBonus;
	}

	/** Damage of a skill (real value): the rank's base plus the servant's own bonus. */
	public double skillDamage(Rank rank, double servantBonus) {
		return this.skillBaseDamage(rank) + servantBonus;
	}

	/** Mana a burst NP costs when the NP sets no cost of its own: damage × rank rate. */
	public double burstDefaultCost(Rank rank, double damage) {
		return damage * this.manaRate(rank);
	}

	public double beamDamagePerSecond(Rank rank) {
		return this.beamDamagePerSecondPerNpBase * this.npBaseDamage(rank);
	}

	/** Mana the attacker pays per second while holding a beam: damage per second × rank rate. */
	public double beamDrainPerSecond(Rank rank) {
		return this.beamDamagePerSecond(rank) * this.manaRate(rank);
	}

	public double shieldHp(Rank rank) {
		return this.shieldHpPerNpBase * this.npBaseDamage(rank);
	}

	/** Mana to cast a shield: its HP × the shield's rate. */
	public double shieldCastCost(Rank rank) {
		return this.shieldHp(rank) * this.manaRate(rank);
	}

	/** Mana to repair {@code hp} points of a shield: HP × the shield's rate. */
	public double shieldRepairCost(Rank rank, double hp) {
		return hp * this.manaRate(rank);
	}
}
