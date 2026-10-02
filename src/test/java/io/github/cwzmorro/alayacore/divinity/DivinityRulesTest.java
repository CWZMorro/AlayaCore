package io.github.cwzmorro.alayacore.divinity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DivinityRulesTest {
	@Test
	void gilgameshStepsFromOneToFiveAtEvenSyncPoints() {
		// plan 10: 1 at 0%, 2 at 25%, 3 at 50%, 4 at 75%, 5 at 100%
		assertEquals(1, DivinityRules.servantDivinity(5, 0.0));
		assertEquals(1, DivinityRules.servantDivinity(5, 0.24));
		assertEquals(2, DivinityRules.servantDivinity(5, 0.25));
		assertEquals(3, DivinityRules.servantDivinity(5, 0.5));
		assertEquals(4, DivinityRules.servantDivinity(5, 0.75));
		assertEquals(4, DivinityRules.servantDivinity(5, 0.99));
		assertEquals(5, DivinityRules.servantDivinity(5, 1.0));
	}

	@Test
	void servantsWithoutDivinityHaveNone() {
		assertEquals(0, DivinityRules.servantDivinity(0, 1.0));
	}

	@Test
	void maxOneNeverChanges() {
		assertEquals(1, DivinityRules.servantDivinity(1, 0.0));
		assertEquals(1, DivinityRules.servantDivinity(1, 1.0));
	}
}
