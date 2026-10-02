package io.github.cwzmorro.alayacore.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The defaults must be plan.md 13.7. */
class RankConfigTest {
	private static final double EPS = 1e-9;
	private static final RankConfig DEFAULTS = RankConfig.DEFAULTS;

	@Test
	void npBaseDamageTable() {
		double[][] plan = {
			{30, 38, 47}, {60, 75, 95}, {119, 143, 171}, {204, 244, 292}, {350, 415, 492}, {583, 692, 820}
		};
		for (RankLetter letter : RankLetter.values()) {
			for (int p = 0; p <= Rank.MAX_PLUSES; p++) {
				assertEquals(plan[letter.ordinal()][p], DEFAULTS.npBaseDamage(new Rank(letter, p)), EPS, letter + "+" + p);
			}
		}
	}

	@Test
	void skillBaseDamageTable() {
		double[][] plan = {
			{20, 22, 24}, {26, 29, 32}, {35, 38, 42}, {46, 50, 55}, {60, 70, 80}, {90, 107, 127}
		};
		for (RankLetter letter : RankLetter.values()) {
			for (int p = 0; p <= Rank.MAX_PLUSES; p++) {
				assertEquals(plan[letter.ordinal()][p], DEFAULTS.skillBaseDamage(new Rank(letter, p)), EPS, letter + "+" + p);
			}
		}
	}

	@Test
	void manaRateBelongsToTheLetter() {
		assertEquals(1.0, DEFAULTS.manaRate(Rank.parse("EX")), EPS);
		assertEquals(1.5, DEFAULTS.manaRate(Rank.parse("A")), EPS);
		assertEquals(2.25, DEFAULTS.manaRate(Rank.parse("B")), EPS);
		assertEquals(3.4, DEFAULTS.manaRate(Rank.parse("C")), EPS);
		assertEquals(5.1, DEFAULTS.manaRate(Rank.parse("D")), EPS);
		assertEquals(7.6, DEFAULTS.manaRate(Rank.parse("E")), EPS);
		assertEquals(DEFAULTS.manaRate(Rank.parse("B")), DEFAULTS.manaRate(Rank.parse("B++")), EPS);
	}

	@Test
	void beamAndSkillDefaults() {
		assertEquals(15, DEFAULTS.beamMaxHoldSeconds());
		assertEquals(30.0, DEFAULTS.beamTurnDegreesPerSecond(), EPS);
		assertEquals(15.0, DEFAULTS.skillDemiMinDamage(), EPS);
	}

	@Test
	void formulas() {
		Rank a = Rank.parse("A");
		Rank bPlus = Rank.parse("B+");
		assertEquals(350 + 12, DEFAULTS.npDamage(a, 12), EPS);
		assertEquals(60 + 5, DEFAULTS.skillDamage(a, 5), EPS);
		assertEquals(525, DEFAULTS.burstDefaultCost(a, 350), EPS);
		assertEquals(70, DEFAULTS.beamDamagePerSecond(a), EPS);
		assertEquals(105, DEFAULTS.beamDrainPerSecond(a), EPS);
		// Rho Aias (plan 9): B+, HP 366, cast cost 366 × 2.25 = 823.5, not rounded
		assertEquals(366, DEFAULTS.shieldHp(bPlus), EPS);
		assertEquals(823.5, DEFAULTS.shieldCastCost(bPlus), EPS);
		assertEquals(100 * 2.25, DEFAULTS.shieldRepairCost(bPlus, 100), EPS);
	}
}
