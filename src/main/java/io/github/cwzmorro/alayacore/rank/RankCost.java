package io.github.cwzmorro.alayacore.rank;

import io.github.cwzmorro.alayacore.config.ConfigSection;

/**
 * Interpolates mana costs geometrically within the E–B++ and A–EX++ tiers, rounding each tier independently.
 * A rounding step of zero leaves costs unrounded.
 *
 * @param e                 cost of E, the first normal step
 * @param highestNormal     cost of B++, the last normal step
 * @param a                 cost of A, the first strongest step
 * @param exHighest         cost of EX++, the last strongest step
 * @param normalRounding    rounding step of the normal tier
 * @param strongestRounding rounding step of the strongest tier
 */
public record RankCost(double e, double highestNormal, double a, double exHighest, double normalRounding, double strongestRounding) {
	private static final RankLetter LAST_NORMAL = RankLetter.B;
	private static final int FIRST_STRONGEST = (LAST_NORMAL.ordinal() + 1) * (Rank.MAX_PLUSES + 1);

	public static RankCost read(ConfigSection s, RankCost defaults) {
		return new RankCost(
			s.number("e", " Cost of E.", defaults.e, 1.0, Double.MAX_VALUE),
			s.number("b_plus_plus", " Cost of B++; E to B++ grow evenly (geometric).", defaults.highestNormal, 1.0, Double.MAX_VALUE),
			s.number("a", " Cost of A.", defaults.a, 1.0, Double.MAX_VALUE),
			s.number("ex_plus_plus", " Cost of EX++; A to EX++ grow evenly (geometric).", defaults.exHighest, 1.0, Double.MAX_VALUE),
			s.number("normal_rounding", " Steps from E to B++ are rounded to a multiple of this (0 = not rounded).",
				defaults.normalRounding, 0.0, Double.MAX_VALUE),
			s.number("strongest_rounding", " Steps from A to EX++ are rounded to a multiple of this (0 = not rounded).",
				defaults.strongestRounding, 0.0, Double.MAX_VALUE)
		);
	}

	public double of(Rank rank) {
		boolean strongest = rank.letter().compareTo(LAST_NORMAL) > 0;
		int first = strongest ? FIRST_STRONGEST : 0;
		int last = strongest ? Rank.STEPS - 1 : FIRST_STRONGEST - 1;
		double from = strongest ? this.a : this.e;
		double to = strongest ? this.exHighest : this.highestNormal;
		double raw = from * Math.pow(to / from, (double) (rank.step() - first) / (last - first));
		double rounding = strongest ? this.strongestRounding : this.normalRounding;
		return rounding > 0.0 ? Math.round(raw / rounding) * rounding : raw;
	}
}
