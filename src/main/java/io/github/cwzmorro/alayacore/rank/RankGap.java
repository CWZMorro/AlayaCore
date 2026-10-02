package io.github.cwzmorro.alayacore.rank;

/** How far apart two ranks are, counted in letters only (plan 9). */
public enum RankGap {
	/** Same letter (A vs A++): both hold; the first to stop or run out of mana loses. */
	SAME,
	/** One letter apart (A vs B, A vs EX): they clash; the lower can win with more mana but drains 1.5×. */
	CLOSE,
	/** Two or more letters apart (B++ vs EX): the higher wins regardless of mana. */
	FAR
}
