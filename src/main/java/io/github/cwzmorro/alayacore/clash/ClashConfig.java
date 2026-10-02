package io.github.cwzmorro.alayacore.clash;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankGap;
import io.github.cwzmorro.alayacore.rank.RankRules;
import io.github.cwzmorro.alayacore.util.Percent;

/** The {@code [clash]} section of {@code config/alayacore.toml} (plan 9). */
public record ClashConfig(double closeLowerDrainMultiplier, double loserHitPercent) {
	public static final ClashConfig DEFAULTS = new ClashConfig(1.5, 90.0);

	public static ClashConfig read(ConfigSection s) {
		return new ClashConfig(
			s.number("close_lower_drain_multiplier", " In a clash one letter apart, the lower rank pays this × its own attack's drain.",
				DEFAULTS.closeLowerDrainMultiplier, 1.0, Double.MAX_VALUE),
			s.number("loser_hit_percent", " Part of the winning attack's damage the loser of a clash takes.",
				DEFAULTS.loserHitPercent, 0.0, Percent.WHOLE)
		);
	}

	/**
	 * Mana per second one side pays in a clash: its own attack's drain, times
	 * {@link #closeLowerDrainMultiplier} when its letter is one below the other's.
	 */
	public double drainPerSecond(double ownDrainPerSecond, Rank own, Rank other) {
		boolean closeAndLower = RankRules.gap(own, other) == RankGap.CLOSE && RankRules.compareLetters(own, other) < 0;
		return closeAndLower ? ownDrainPerSecond * this.closeLowerDrainMultiplier : ownDrainPerSecond;
	}

	/** The damage the loser takes from the winning attack's {@code damage}. */
	public double loserDamage(double damage) {
		return damage * Percent.toFraction(this.loserHitPercent);
	}
}
