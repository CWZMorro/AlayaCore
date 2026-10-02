package io.github.cwzmorro.alayacore.divinity;

/** Divinity numbers (plan 10). Plain Java, no game calls. */
public final class DivinityRules {
	/** Full god. */
	public static final int MAX = 10;
	/** A servant with any divinity starts here and steps up to its max with sync. */
	private static final int SERVANT_START = 1;

	private DivinityRules() {
	}

	/**
	 * A servant's divinity: none if the servant has none; else {@value #SERVANT_START} at 0% sync, one
	 * more at each even sync step, its max at 100% (max 5: 1, 2, 3, 4, 5 at 0, 25, 50, 75, 100%).
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
