package io.github.cwzmorro.alayacore.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RankConfigTest {
	private static final double EPS = 1e-9;
	private static final RankConfig DEFAULTS = RankConfig.DEFAULTS;

	@Test
	void npBaseDamageTable() {
		double[][] expected = {
			{30, 38, 47}, {60, 75, 95}, {119, 143, 171}, {204, 244, 292}, {350, 415, 492}, {583, 692, 820}
		};
		for (RankLetter letter : RankLetter.values()) {
			for (int p = 0; p <= Rank.MAX_PLUSES; p++) {
				assertEquals(expected[letter.ordinal()][p], DEFAULTS.npBaseDamage(new Rank(letter, p)), EPS, letter + "+" + p);
			}
		}
	}

	@Test
	void skillBaseDamageTable() {
		double[][] expected = {
			{20, 22, 24}, {26, 29, 32}, {35, 38, 42}, {46, 50, 55}, {60, 70, 80}, {90, 107, 127}
		};
		for (RankLetter letter : RankLetter.values()) {
			for (int p = 0; p <= Rank.MAX_PLUSES; p++) {
				assertEquals(expected[letter.ordinal()][p], DEFAULTS.skillBaseDamage(new Rank(letter, p)), EPS, letter + "+" + p);
			}
		}
	}

	@Test
	void repairRateBelongsToTheLetterAndFallsAtEveryStep() {
		assertEquals(1.0, DEFAULTS.repairRate(Rank.parse("EX")), EPS);
		assertEquals(1.5, DEFAULTS.repairRate(Rank.parse("A")), EPS);
		assertEquals(2.25, DEFAULTS.repairRate(Rank.parse("B")), EPS);
		assertEquals(3.4, DEFAULTS.repairRate(Rank.parse("C")), EPS);
		assertEquals(5.1, DEFAULTS.repairRate(Rank.parse("D")), EPS);
		assertEquals(7.6, DEFAULTS.repairRate(Rank.parse("E")), EPS);
		assertEquals(1.5 - 0.5 / 3, DEFAULTS.repairRate(Rank.parse("A+")), EPS);
		assertEquals(1.5 - 1.0 / 3, DEFAULTS.repairRate(Rank.parse("A++")), EPS);
		assertEquals(1.0 - 0.5 / 3, DEFAULTS.repairRate(Rank.parse("EX+")), EPS); // 0.833
		assertEquals(1.0 - 1.0 / 3, DEFAULTS.repairRate(Rank.parse("EX++")), EPS); // 0.667
		double before = Double.MAX_VALUE;
		for (RankLetter letter : RankLetter.values()) {
			for (int p = 0; p <= Rank.MAX_PLUSES; p++) {
				double rate = DEFAULTS.repairRate(new Rank(letter, p));
				assertTrue(rate < before || before == Double.MAX_VALUE, letter + "+" + p);
				before = rate;
			}
		}
	}

	@Test
	void costTablesMatchTheDefaults() {
		double[][] expected = {
			{50, 55, 60, 65, 75, 80, 90, 100, 110, 125, 135, 150, 200, 215, 235, 255, 275, 300},
			{150, 180, 210, 240, 290, 340, 400, 470, 550, 650, 760, 900, 1500, 1770, 2080, 2450, 2890, 3400},
			{10, 10, 15, 15, 20, 25, 25, 30, 35, 45, 50, 60, 130, 150, 170, 200, 230, 260},
			{300, 320, 350, 380, 410, 440, 480, 510, 560, 600, 650, 700, 1500, 1660, 1840, 2040, 2260, 2500}
		};
		for (RankLetter letter : RankLetter.values()) {
			for (int p = 0; p <= Rank.MAX_PLUSES; p++) {
				Rank rank = new Rank(letter, p);
				int step = rank.step();
				assertEquals(expected[0][step], DEFAULTS.skillCost(rank), EPS, "skill " + rank);
				assertEquals(expected[1][step], DEFAULTS.burstCost(rank), EPS, "burst " + rank);
				assertEquals(expected[2][step], DEFAULTS.beamDrainPerSecond(rank), EPS, "beam " + rank);
				assertEquals(expected[3][step], DEFAULTS.shieldCastCost(rank), EPS, "shield " + rank);
			}
		}
	}

	@Test
	void anEndpointMovesItsTiersSteps() {
		RankCost row = new RankCost(100, 800, 1000, 4000, 0, 0);
		assertEquals(100, row.of(Rank.parse("E")), EPS);
		assertEquals(800, row.of(Rank.parse("B++")), EPS);
		assertEquals(1000, row.of(Rank.parse("A")), EPS);
		assertEquals(4000, row.of(Rank.parse("EX++")), EPS);
		// Unrounded, the middle of a tier is the geometric mean: E+ of 11 steps, EX of 5 steps.
		assertEquals(100 * Math.pow(8, 1.0 / 11), row.of(Rank.parse("E+")), EPS);
		assertEquals(1000 * Math.pow(4, 3.0 / 5), row.of(Rank.parse("EX")), EPS);
		RankCost raised = new RankCost(100, 1600, 1000, 4000, 0, 0);
		assertTrue(raised.of(Rank.parse("C")) > row.of(Rank.parse("C")));
		assertEquals(row.of(Rank.parse("A+")), raised.of(Rank.parse("A+")), EPS);
		RankCost rounded = new RankCost(100, 800, 1000, 4000, 25, 100);
		assertEquals(0, rounded.of(Rank.parse("C")) % 25, EPS);
		assertEquals(0, rounded.of(Rank.parse("EX")) % 100, EPS);
	}

	@Test
	void beamAndSkillDefaults() {
		assertEquals(15, DEFAULTS.beamMaxHoldSeconds());
		assertEquals(30.0, DEFAULTS.beamTurnDegreesPerSecond(), EPS);
	}

	@Test
	void formulas() {
		Rank a = Rank.parse("A");
		Rank bPlus = Rank.parse("B+");
		assertEquals(350 + 12, DEFAULTS.npDamage(a, 12), EPS);
		assertEquals(60 + 5, DEFAULTS.skillDamage(a, 5), EPS);
		assertEquals(1500, DEFAULTS.burstCost(a), EPS);
		assertEquals(70, DEFAULTS.beamDamagePerSecond(a), EPS);
		assertEquals(130, DEFAULTS.beamDrainPerSecond(a), EPS);
		assertEquals(366, DEFAULTS.shieldHp(bPlus), EPS);
		assertEquals(650, DEFAULTS.shieldCastCost(bPlus), EPS);
		assertEquals(100 * DEFAULTS.repairRate(bPlus), DEFAULTS.shieldRepairCost(bPlus, 100), EPS);
	}
}
