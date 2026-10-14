package io.github.cwzmorro.alayacore.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.cwzmorro.alayacore.rank.Rank;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.Test;

class ShieldRulesTest {
	private static final double EPS = 1e-9;
	private static final Rank A = Rank.parse("A");

	@Test
	void farAboveBreaksItAtOnce() {
		assertEquals(0.0, ShieldRules.hpAfterHit(Rank.parse("EX"), Rank.parse("B"), 306, 1), EPS);
	}

	@Test
	void everyOtherAttackTakesHp() {
		assertEquals(425.0, ShieldRules.hpAfterHit(Rank.parse("EX"), A, 525, 100), EPS);
		assertEquals(425.0, ShieldRules.hpAfterHit(Rank.parse("A++"), A, 525, 100), EPS);
		assertEquals(425.0, ShieldRules.hpAfterHit(Rank.parse("C"), A, 525, 100), EPS);
		assertEquals(425.0, ShieldRules.hpAfterHit(null, A, 525, 100), EPS);
		assertEquals(0.0, ShieldRules.hpAfterHit(null, A, 50, 100), EPS);
	}

	@Test
	void aTappedShieldStaysFiveMinutes() {
		assertEquals(300 * SharedConstants.TICKS_PER_SECOND, ShieldConfig.DEFAULTS.tapTicks());
	}

	@Test
	void repairIsWhatIsMissingOrWhatTheManaPaysFor() {
		assertEquals(100.0, ShieldRules.repairable(425, 525, 1000, 1.5), EPS);
		assertEquals(20.0, ShieldRules.repairable(425, 525, 30, 1.5), EPS);
		assertEquals(0.0, ShieldRules.repairable(525, 525, 1000, 1.5), EPS);
		assertEquals(0.0, ShieldRules.repairable(425, 525, 0, 1.5), EPS);
	}
}
