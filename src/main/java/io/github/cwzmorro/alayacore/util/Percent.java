package io.github.cwzmorro.alayacore.util;

/** Percentages as the config and the HUD write them (50 = half). */
public final class Percent {
	/** 100%: the upper limit of a percent value in the config. */
	public static final double WHOLE = 100.0;

	private Percent() {
	}

	/** 50 → 0.5. */
	public static double toFraction(double percent) {
		return percent / WHOLE;
	}

	/** 0.5 → 50. */
	public static double fromFraction(double fraction) {
		return fraction * WHOLE;
	}
}
