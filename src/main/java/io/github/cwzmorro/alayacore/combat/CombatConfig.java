package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.rank.RankLetter;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The {@code [combat]} section of {@code config/alayacore.toml}: what servant damage ignores of the target's
 * defence, and the melee reference speed of the servant bonus.
 *
 * @param servantProtectionIgnorePercent part of the Protection enchantment all servant damage ignores
 * @param meleeReferenceAttacksPerSecond a melee hit's class attack bonus is the class attack × this ÷ the attacker's attack speed
 * @param npArmorIgnoredPercent per rank letter of a noble phantasm: the part of the armor it ignores (a missing letter ignores none)
 * @param npProtectionIgnoredPercent per rank letter of a noble phantasm: the part of the Protection it ignores
 * @param ignoresDefenceArmorPercent the part of the armor reduction a {@link AttackDefence#IGNORES_DEFENCE} damage type ignores
 * @param ignoresDefenceProtectionPercent the part of the Protection (after vanilla's cap of 20) such a damage type ignores
 */
public record CombatConfig(double servantProtectionIgnorePercent, double meleeReferenceAttacksPerSecond,
	Map<RankLetter, Double> npArmorIgnoredPercent, Map<RankLetter, Double> npProtectionIgnoredPercent,
	double ignoresDefenceArmorPercent, double ignoresDefenceProtectionPercent) {
	public static final CombatConfig DEFAULTS = new CombatConfig(50.0, 1.6,
		tiers(25.0, 50.0, 100.0), tiers(50.0, 75.0, 100.0), 90.0, 50.0);

	private static Map<RankLetter, Double> tiers(double b, double a, double ex) {
		Map<RankLetter, Double> map = new EnumMap<>(RankLetter.class);
		map.put(RankLetter.B, b);
		map.put(RankLetter.A, a);
		map.put(RankLetter.EX, ex);
		return Collections.unmodifiableMap(map);
	}

	public static CombatConfig read(ConfigSection s) {
		return new CombatConfig(
			s.number("servant_protection_ignore_percent", " Part of the target's Protection enchantment that all servant damage ignores.",
				DEFAULTS.servantProtectionIgnorePercent, 0.0, Percent.WHOLE),
			s.number("melee_reference_attacks_per_second", " A servant's melee hit adds its class attack × this ÷ its attack speed (no attack speed: the whole class attack).",
				DEFAULTS.meleeReferenceAttacksPerSecond, 0.1, 20.0),
			readTiers(s.inlineSection("np_armor_ignored_percent", " Part of the armor a noble phantasm ignores, by rank letter (a skill counts one letter lower)."),
				DEFAULTS.npArmorIgnoredPercent),
			readTiers(s.inlineSection("np_protection_ignored_percent", " Part of the Protection enchantment a noble phantasm ignores, by rank letter."),
				DEFAULTS.npProtectionIgnoredPercent),
			s.number("ignores_defence_armor_percent", " Part of what armor would take off that damage types tagged alayacore:ignores_defence ignore.",
				DEFAULTS.ignoresDefenceArmorPercent, 0.0, Percent.WHOLE),
			s.number("ignores_defence_protection_percent", " Part of the Protection (after vanilla's limit of 20) that damage types tagged alayacore:ignores_defence ignore.",
				DEFAULTS.ignoresDefenceProtectionPercent, 0.0, Percent.WHOLE)
		);
	}

	private static Map<RankLetter, Double> readTiers(ConfigSection table, Map<RankLetter, Double> fallback) {
		Map<RankLetter, Double> map = new EnumMap<>(RankLetter.class);
		for (Map.Entry<RankLetter, Double> entry : fallback.entrySet()) {
			map.put(entry.getKey(), table.number(entry.getKey().name(), null, entry.getValue(), 0.0, Percent.WHOLE));
		}
		return Collections.unmodifiableMap(map);
	}

	public double servantProtectionIgnored() {
		return Percent.toFraction(this.servantProtectionIgnorePercent);
	}

	public double ignoresDefenceArmor() {
		return Percent.toFraction(this.ignoresDefenceArmorPercent);
	}

	public double ignoresDefenceProtection() {
		return Percent.toFraction(this.ignoresDefenceProtectionPercent);
	}

	public double npArmorIgnored(RankLetter letter) {
		return Percent.toFraction(this.npArmorIgnoredPercent.getOrDefault(letter, 0.0));
	}

	public double npProtectionIgnored(RankLetter letter) {
		return Percent.toFraction(this.npProtectionIgnoredPercent.getOrDefault(letter, 0.0));
	}
}
