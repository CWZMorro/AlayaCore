package io.github.cwzmorro.alayacore.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cwzmorro.alayacore.rank.RankLetter;
import org.junit.jupiter.api.Test;

class CombatConfigTest {
	private static final double EPS = 1e-9;
	private static final CombatConfig DEFAULTS = CombatConfig.DEFAULTS;

	@Test
	void servantDamageIgnoresHalfTheProtection() {
		assertEquals(0.5, DEFAULTS.servantProtectionIgnored(), EPS);
	}

	@Test
	void noblePhantasmsIgnoreArmorAndProtectionByLetter() {
		assertEquals(0.0, DEFAULTS.npArmorIgnored(RankLetter.C), EPS);
		assertEquals(0.25, DEFAULTS.npArmorIgnored(RankLetter.B), EPS);
		assertEquals(0.5, DEFAULTS.npArmorIgnored(RankLetter.A), EPS);
		assertEquals(1.0, DEFAULTS.npArmorIgnored(RankLetter.EX), EPS);
		assertEquals(0.0, DEFAULTS.npProtectionIgnored(RankLetter.C), EPS);
		assertEquals(0.5, DEFAULTS.npProtectionIgnored(RankLetter.B), EPS);
		assertEquals(0.75, DEFAULTS.npProtectionIgnored(RankLetter.A), EPS);
		assertEquals(1.0, DEFAULTS.npProtectionIgnored(RankLetter.EX), EPS);
	}

	@Test
	void aMeleeHitsClassAttackIsScaledByOneAndSixTenthsOverTheAttackSpeed() {
		assertEquals(1.6, DEFAULTS.meleeReferenceAttacksPerSecond(), EPS);
	}
}
