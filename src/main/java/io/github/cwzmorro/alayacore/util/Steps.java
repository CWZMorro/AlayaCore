package io.github.cwzmorro.alayacore.util;

/** Numbers chosen in steps, as an ability setting or a slider offers them. */
public final class Steps {
	private Steps() {
	}

	public static double snap(double min, double max, double step, double value) {
		return Math.clamp(min + Math.round((value - min) / step) * step, min, max);
	}
}
