package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.util.Percent;

/**
 * The tunable values of one servant class, under {@code [classes."<id>"]} in the config.
 *
 * @param real                  the class's real values (plan 13.7), reached at sync 100% + true name
 * @param manaRegenBonusPercent extra mana regeneration of all kinds, in percent (Archer: 20, plan 7)
 * @param regenWhileCasting     mana keeps regenerating while casting or holding an ability (Archer, plan 7)
 */
public record ServantClassConfig(ServantStats real, double manaRegenBonusPercent, boolean regenWhileCasting) {
	public static ServantClassConfig read(ConfigSection s, ServantClassConfig defaults) {
		return new ServantClassConfig(
			ServantStats.read(s, defaults.real),
			s.number("mana_regen_bonus_percent", null, defaults.manaRegenBonusPercent, 0.0, Double.MAX_VALUE),
			s.bool("regen_while_casting", null, defaults.regenWhileCasting)
		);
	}

	/** Multiplier for every kind of mana regeneration. */
	public double manaRegenMultiplier() {
		return 1.0 + Percent.toFraction(this.manaRegenBonusPercent);
	}
}
