package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.config.ConfigSection;

/**
 * The stats a servant's progression moves between (plan 13.2, 13.7). Attack, armor, toughness and
 * knockback resistance add to the player's own and their gear; speed is a percent over walking speed.
 */
public record ServantStats(double maxHp, double attack, double armor, double toughness, double speedPercent, double knockbackResistance) {
	/** Vanilla's attribute limits for the bonuses (armor 30, toughness 20, knockback resistance 1). */
	private static final double MAX_ARMOR = 30.0;
	private static final double MAX_TOUGHNESS = 20.0;
	private static final double MAX_KNOCKBACK_RESISTANCE = 1.0;
	/** Vanilla's max health limit. */
	private static final double MAX_HEALTH = 1024.0;

	public static ServantStats read(ConfigSection s, ServantStats defaults) {
		return new ServantStats(
			s.number("max_hp", null, defaults.maxHp, 1.0, MAX_HEALTH),
			s.number("attack", null, defaults.attack, 0.0, Double.MAX_VALUE),
			s.number("armor", null, defaults.armor, 0.0, MAX_ARMOR),
			s.number("toughness", null, defaults.toughness, 0.0, MAX_TOUGHNESS),
			s.number("speed_percent", null, defaults.speedPercent, 0.0, Double.MAX_VALUE),
			s.number("knockback_resistance", null, defaults.knockbackResistance, 0.0, MAX_KNOCKBACK_RESISTANCE)
		);
	}

	/** The point {@code fill} (0–1) of the way from {@code demi} to {@code real}. */
	public static ServantStats between(ServantStats demi, ServantStats real, double fill) {
		return new ServantStats(
			lerp(demi.maxHp, real.maxHp, fill),
			lerp(demi.attack, real.attack, fill),
			lerp(demi.armor, real.armor, fill),
			lerp(demi.toughness, real.toughness, fill),
			lerp(demi.speedPercent, real.speedPercent, fill),
			lerp(demi.knockbackResistance, real.knockbackResistance, fill)
		);
	}

	private static double lerp(double from, double to, double fill) {
		return from + (to - from) * fill;
	}
}
