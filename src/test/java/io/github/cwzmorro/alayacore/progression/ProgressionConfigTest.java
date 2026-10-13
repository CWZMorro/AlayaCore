package io.github.cwzmorro.alayacore.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cwzmorro.alayacore.servant.ServantStats;
import org.junit.jupiter.api.Test;

class ProgressionConfigTest {
	private static final double EPS = 1e-9;
	private static final ProgressionConfig DEFAULTS = ProgressionConfig.DEFAULTS;
	private static final ServantStats SABER = new ServantStats(350, 36, 4, 2, 20, 0.7);

	@Test
	void syncFromMaxMana() {
		assertEquals(0.0, DEFAULTS.sync(500), EPS);
		assertEquals(0.0, DEFAULTS.sync(1_000), EPS);
		assertEquals(0.5, DEFAULTS.sync(Math.sqrt(10_000 * 1_000)), EPS); // 3,162 ≈ 50%
		assertEquals(1.0, DEFAULTS.sync(10_000), EPS);
		assertEquals(1.0, DEFAULTS.sync(12_000), EPS); // mana keeps growing to the ceiling, sync stays 100%
		assertEquals(Math.log10(3), DEFAULTS.sync(3_000), EPS); // 3k ≈ 48%
	}

	@Test
	void barFill() {
		assertEquals(0.0, DEFAULTS.barFill(0, false), EPS);
		assertEquals(0.175, DEFAULTS.barFill(0.5, false), EPS);
		assertEquals(0.7, DEFAULTS.barFill(1, false), EPS);
		assertEquals(1.0, DEFAULTS.barFill(1, true), EPS);
		assertEquals(0.3, DEFAULTS.barFill(0, true), EPS);
	}

	@Test
	void classGrades() {
		assertEquals(ClassGrade.BLACK, DEFAULTS.grade(0.49, true));
		assertEquals(ClassGrade.BRONZE, DEFAULTS.grade(0.5, false));
		assertEquals(ClassGrade.BRONZE, DEFAULTS.grade(0.99, true));
		assertEquals(ClassGrade.SILVER, DEFAULTS.grade(1.0, false));
		assertEquals(ClassGrade.GOLD, DEFAULTS.grade(1.0, true));
	}

	@Test
	void saberAtEachStage() {
		assertEquals(new ServantStats(30, 2, 2, 0, 5, 0), ServantStats.between(DEFAULTS.demi(), SABER, DEFAULTS.barFill(0, false)));
		ServantStats half = ServantStats.between(DEFAULTS.demi(), SABER, DEFAULTS.barFill(0.5, false));
		assertEquals(86.0, half.maxHp(), EPS);
		assertEquals(7.95, half.attack(), EPS);
		assertEquals(SABER, ServantStats.between(DEFAULTS.demi(), SABER, DEFAULTS.barFill(1, true)));
	}
}
