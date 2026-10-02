package io.github.cwzmorro.alayacore.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbilityRulesTest {
	private static final double EPS = 1e-9;

	@Test
	void exclusivity() {
		// plan 8: abilities run side by side unless one of them is exclusive
		assertFalse(AbilityRules.isBlocked(false, false, false));
		assertFalse(AbilityRules.isBlocked(false, true, false));
		assertFalse(AbilityRules.isBlocked(true, false, false));
		assertTrue(AbilityRules.isBlocked(true, true, false), "an exclusive ability can't join others");
		assertTrue(AbilityRules.isBlocked(false, true, true), "nothing joins an exclusive ability");
	}

	@Test
	void tapOrHold() {
		// plan 8: 0.25 s = 5 ticks
		int threshold = AbilityRules.toTicks(0.25);
		assertEquals(5, threshold);
		assertFalse(AbilityRules.isHold(threshold - 1, threshold));
		assertTrue(AbilityRules.isHold(threshold, threshold));
	}

	@Test
	void manaAndCooldown() {
		assertTrue(AbilityRules.canAfford(100, 100));
		assertFalse(AbilityRules.canAfford(99.5, 100));
		assertEquals(1.5, AbilityRules.secondsLeft(130, 100), EPS);
		assertEquals(0, AbilityRules.secondsLeft(100, 130), EPS);
	}
}
