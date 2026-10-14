package io.github.cwzmorro.alayacore.combat;

import net.minecraft.SharedConstants;

/**
 * Distributes per-second weapon damage across periodic pulses rather than continuous ticks.
 */
public final class WeaponHits {
	private static final int INTERVAL_TICKS = 10;

	private WeaponHits() {
	}

	public static boolean lands(int ticks) {
		return ticks % INTERVAL_TICKS == 0;
	}

	public static float damage(double perSecond) {
		return (float) (perSecond * INTERVAL_TICKS / SharedConstants.TICKS_PER_SECOND);
	}
}
