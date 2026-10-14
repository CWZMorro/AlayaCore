package io.github.cwzmorro.alayacore.thrown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThrownWeaponConfigTest {
	private static final double EPS = 1e-9;
	private static final ThrownWeaponConfig DEFAULTS = ThrownWeaponConfig.DEFAULTS;

	@Test
	void defaultsMatchTheDesignedValues() {
		assertEquals(3.0, DEFAULTS.blocksPerTick(), EPS);
		assertEquals(2, DEFAULTS.hitImmunityTicks());
		assertEquals(4.0, DEFAULTS.spearDivisor(), EPS);
		assertEquals(600, DEFAULTS.lifetimeTicks());
	}
}
