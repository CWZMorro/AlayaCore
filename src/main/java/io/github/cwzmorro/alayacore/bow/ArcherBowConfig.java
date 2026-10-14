package io.github.cwzmorro.alayacore.bow;

import io.github.cwzmorro.alayacore.config.ConfigSection;

/** The {@code [archer_bows]} section of {@code config/alayacore.toml}: how an Archer's bar speeds its bow. */
public record ArcherBowConfig(double archerDrawReduction, double archerSpeedBonus) {
	public static final ArcherBowConfig DEFAULTS = new ArcherBowConfig(10.0, 0.25);
	public static final String ARCHER_DRAW_REDUCTION = "archer_draw_reduction";
	public static final String ARCHER_SPEED_BONUS = "archer_speed_bonus";

	public static ArcherBowConfig read(ConfigSection s) {
		return new ArcherBowConfig(
			s.number(ARCHER_DRAW_REDUCTION, " Ticks removed from a vanilla full draw at full bar.", DEFAULTS.archerDrawReduction, 0.0, 19.0),
			s.number(ARCHER_SPEED_BONUS, " Ordinary bow arrow speed bonus at full bar.", DEFAULTS.archerSpeedBonus, 0.0, 10.0));
	}
}
