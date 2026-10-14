package io.github.cwzmorro.alayacore.divinity;

/** Divinity numbers. No game calls. */
public final class DivinityRules {
	public static final int MAX = 10;

	private DivinityRules() {
	}

	/**
	 * Returns divinity in evenly spaced integer steps from start to max as sync rises.
	 *
	 * @param sync 0 to 1
	 */
	public static int servantDivinity(int start, int max, double sync) {
		int steps = max - start;
		return start + (int) Math.floor(Math.clamp(sync, 0.0, 1.0) * steps);
	}
}
