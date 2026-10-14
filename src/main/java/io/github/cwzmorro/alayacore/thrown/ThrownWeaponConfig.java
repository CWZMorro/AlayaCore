package io.github.cwzmorro.alayacore.thrown;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.util.Durations;

/**
 * The {@code [thrown_weapons]} section of {@code config/alayacore.toml}: how every launched weapon flies and
 * strikes, whoever launched it, and how long a used-up one that strikes nothing flies on.
 */
public record ThrownWeaponConfig(double blocksPerTick, int hitImmunityTicks, double spearDivisor, double lifetimeSeconds) {
	public static final ThrownWeaponConfig DEFAULTS = new ThrownWeaponConfig(3.0, 2, 4.0, 30.0);
	public static final String BLOCKS_PER_TICK = "blocks_per_tick";
	public static final String HIT_IMMUNITY_TICKS = "hit_immunity_ticks";
	public static final String SPEAR_DIVISOR = "spear_divisor";
	public static final String LIFETIME_SECONDS = "lifetime_seconds";
	private static final double MAX_LIFETIME_SECONDS = 3600.0;

	public static ThrownWeaponConfig read(ConfigSection s) {
		return new ThrownWeaponConfig(
			s.number(BLOCKS_PER_TICK, " How fast everything thrown flies, straight, never falling (3.0: a fully charged bow).",
				DEFAULTS.blocksPerTick, 0.1, 10.0),
			s.integer(HIT_IMMUNITY_TICKS, " Ticks a target is immune to thrown weapons after a hit, instead of vanilla's half second (2: 0.1 s).",
				DEFAULTS.hitImmunityTicks, 0, ThrownWeaponDamage.VANILLA_HIT_IMMUNITY_TICKS),
			s.number(SPEAR_DIVISOR, " A thrown spear's speed damage is divided by this.", DEFAULTS.spearDivisor, 1.0, 100.0),
			s.number(LIFETIME_SECONDS, " Seconds a used-up thrown weapon that strikes nothing flies on after it was thrown or released.",
				DEFAULTS.lifetimeSeconds, 1.0, MAX_LIFETIME_SECONDS)
		);
	}

	public int lifetimeTicks() {
		return Durations.ticks(this.lifetimeSeconds);
	}
}
