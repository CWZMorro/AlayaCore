package io.github.cwzmorro.alayacore.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArcherBowConfigTest {
	private static final double EPS = 1e-9;

	@Test
	void defaultsMatchTheDesignedValues() {
		assertEquals(10.0, ArcherBowConfig.DEFAULTS.archerDrawReduction(), EPS);
		assertEquals(0.25, ArcherBowConfig.DEFAULTS.archerSpeedBonus(), EPS);
	}
}
