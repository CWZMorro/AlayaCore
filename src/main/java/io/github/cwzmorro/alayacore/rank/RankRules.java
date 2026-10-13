package io.github.cwzmorro.alayacore.rank;

/**
 * Comparing two ranks.
 *
 * <p>Only letters count: "+" and "++" never change the gap. The mana drain of a close clash is in
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

	public static int compareLetters(Rank a, Rank b) {
		return a.letter().compareTo(b.letter());
	}

	public static boolean overwhelms(Rank a, Rank b) {
		return gap(a, b) == RankGap.FAR && compareLetters(a, b) > 0;
	}
}
