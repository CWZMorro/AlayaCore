package io.github.cwzmorro.alayacore.beam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeldBeamConfigTest {
	@Test
	void movementIsAFifth() {
		assertEquals(0.2, HeldBeamConfig.DEFAULTS.heldBeamMovementMultiplier(), 1e-9);
	}
}
