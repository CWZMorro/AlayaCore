package io.github.cwzmorro.alayacore.mana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ManaRulesTest {
	private static final double EPS = 1e-9;

	@Test
	void currentStaysBetweenZeroAndMax() {
		ManaData mana = new ManaData(990, 1000);
		assertEquals(1000, ManaRules.addCurrent(mana, 50).current(), EPS);
		assertEquals(0, ManaRules.addCurrent(mana, -5000).current(), EPS);
		assertEquals(993, ManaRules.addCurrent(mana, 3).current(), EPS);
	}

	@Test
	void raisingMaxKeepsCurrent() {
		ManaData after = ManaRules.addMax(new ManaData(400, 1000), 500);
		assertEquals(1500, after.max(), EPS);
		assertEquals(400, after.current(), EPS);
	}

	@Test
	void fractionForTheHud() {
		assertEquals(0.75, ManaRules.fraction(new ManaData(750, 1000)), EPS);
		assertEquals(0, ManaRules.fraction(new ManaData(0, 0)), EPS);
	}

	@Test
	void fractionOfMax() {
		assertEquals(2000, ManaRules.addFractionOfMax(new ManaData(0, 10_000), 0.2).current(), EPS);
	}
}
