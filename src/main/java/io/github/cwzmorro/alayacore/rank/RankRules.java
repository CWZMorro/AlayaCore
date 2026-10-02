package io.github.cwzmorro.alayacore.rank;

/**
 * Comparing two ranks (plan 9).
 *
 * <p>Only letters count: "+" and "++" never change the gap, so B++ vs EX is still far apart and
 * A vs A++ is the same rank (QnA R3 #23). The 1.5× drain of the lower side in a close clash is
 * {@link io.github.cwzmorro.alayacore.clash.ClashConfig#drainPerSecond}.
 */
public final class RankRules {
	private RankRules() {
	}

	public static RankGap gap(Rank a, Rank b) {
		int letters = Math.abs(a.letter().ordinal() - b.letter().ordinal());
		if (letters == 0) {
			return RankGap.SAME;
		}
		return letters == 1 ? RankGap.CLOSE : RankGap.FAR;
	}

	/** Positive if {@code a}'s letter is higher, negative if lower, 0 if the same letter. */
	public static int compareLetters(Rank a, Rank b) {
		return a.letter().compareTo(b.letter());
	}

	/** True if {@code a} wins outright: far apart and higher. */
	public static boolean overwhelms(Rank a, Rank b) {
		return gap(a, b) == RankGap.FAR && compareLetters(a, b) > 0;
	}

	/**
	 * True if the attack wins outright against the defence. An absolute defence (Avalon: EX, but
	 * it blocks everything) is never overwhelmed, whatever the attack's rank.
	 */
	public static boolean overwhelms(Rank attack, Rank defence, boolean absoluteDefence) {
		return !absoluteDefence && overwhelms(attack, defence);
	}
}
