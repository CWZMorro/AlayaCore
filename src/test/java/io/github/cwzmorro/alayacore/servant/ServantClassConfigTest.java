package io.github.cwzmorro.alayacore.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServantClassConfigTest {
	@Test
	void archerBonusIsTwentyPercent() {
		// plan 7: Archer mana regen +20%
		assertEquals(1.2, new ServantClassConfig(new ServantStats(275, 24, 2, 0, 25, 0.4), 20.0, true).manaRegenMultiplier(), 1e-9);
		assertEquals(1.0, new ServantClassConfig(new ServantStats(350, 36, 4, 2, 20, 0.7), 0.0, false).manaRegenMultiplier(), 1e-9);
	}
}
