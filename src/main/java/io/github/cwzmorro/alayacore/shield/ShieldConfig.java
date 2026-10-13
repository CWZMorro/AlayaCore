package io.github.cwzmorro.alayacore.shield;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import net.minecraft.SharedConstants;

/** The {@code [shields]} section of {@code config/alayacore.toml}. HP, cast and repair costs are in {@code [ranks]}. */
public record ShieldConfig(int tapSeconds, double holdUpkeepPerSecond, double projectileReach) {
	public static final ShieldConfig DEFAULTS = new ShieldConfig(300, 5.0, 8.0);

	public static ShieldConfig read(ConfigSection s) {
		return new ShieldConfig(
			s.integer("tap_seconds", " How long a tapped shield stays, and how long it stays after it is let go.", DEFAULTS.tapSeconds, 1,
				Integer.MAX_VALUE),
			s.number("hold_upkeep_per_second", " Mana per second for holding any shield (repair is paid on top).",
				DEFAULTS.holdUpkeepPerSecond, 0.0, Double.MAX_VALUE),
			s.number("projectile_reach", " Farthest a projectile can move in one tick, in blocks, and still be caught by a shield.",
				DEFAULTS.projectileReach, 0.0, Double.MAX_VALUE)
		);
	}

	public int tapTicks() {
		return this.tapSeconds * SharedConstants.TICKS_PER_SECOND;
	}

	public double holdUpkeepPerTick() {
		return this.holdUpkeepPerSecond / SharedConstants.TICKS_PER_SECOND;
	}
}
