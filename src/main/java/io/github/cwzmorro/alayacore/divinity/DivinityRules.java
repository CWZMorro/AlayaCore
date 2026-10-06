package io.github.cwzmorro.alayacore.divinity;

/** Divinity numbers. No game calls. */
public final class DivinityRules {
	public static final int MAX = 10;
	private static final int SERVANT_START = 1;

	private DivinityRules() {
	}

	/**
	 * Returns divinity in evenly spaced integer steps from {@value #SERVANT_START} to max as sync rises, or zero for a max below
	 * that.
	 *
	 * @param sync 0 to 1
	 */
	public static int servantDivinity(int max, double sync) {
		if (max < SERVANT_START) {
			return 0;
		}
		int steps = max - SERVANT_START;
		return SERVANT_START + (int) Math.floor(Math.clamp(sync, 0.0, 1.0) * steps);
	}
}
