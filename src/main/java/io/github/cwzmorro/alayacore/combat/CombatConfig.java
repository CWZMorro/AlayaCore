package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.util.Percent;

/** The {@code [combat]} section of {@code config/alayacore.toml} (plan 13.7). */
public record CombatConfig(double servantProtectionIgnorePercent) {
	public static final CombatConfig DEFAULTS = new CombatConfig(50.0);

	public static CombatConfig read(ConfigSection s) {
		return new CombatConfig(
			s.number("servant_protection_ignore_percent", " Part of the target's Protection enchantment that all servant damage ignores.",
				DEFAULTS.servantProtectionIgnorePercent, 0.0, Percent.WHOLE)
		);
	}

	/** The Protection a servant's attack still meets. */
	public float protectionAgainstServant(float protection) {
		return (float) (protection * (1.0 - Percent.toFraction(this.servantProtectionIgnorePercent)));
	}
}
