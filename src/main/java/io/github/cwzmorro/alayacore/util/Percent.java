package io.github.cwzmorro.alayacore.util;

/** Percentages as the config and the HUD write them (50 = half). */
public final class Percent {
	public static final double WHOLE = 100.0;

	private Percent() {
	}

	public static double toFraction(double percent) {
		return percent / WHOLE;
	}

	public static double fromFraction(double fraction) {
		return fraction * WHOLE;
	}
}
