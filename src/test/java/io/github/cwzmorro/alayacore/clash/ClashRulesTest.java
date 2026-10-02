package io.github.cwzmorro.alayacore.clash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.rank.Rank;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClashRulesTest {
	private static final double EPS = 1e-9;
	private static final double ENDLESS = Double.POSITIVE_INFINITY;

	private static AttackData attack(String rank, boolean canClash) {
		return new AttackData(UUID.randomUUID(), Rank.parse(rank), canClash);
	}

	@Test
	void bothMustBeAbleToClash() {
		assertTrue(ClashRules.clash(attack("A", true), attack("EX", true)));
		assertFalse(ClashRules.clash(attack("A", true), attack("A", false)));
		assertFalse(ClashRules.clash(attack("A", false), attack("A", true)));
	}

	@Test
	void farApartTheHigherWinsAtOnce() {
		assertEquals(ClashRules.Side.A, ClashRules.outrightWinner(Rank.parse("EX"), Rank.parse("B")));
		assertEquals(ClashRules.Side.B, ClashRules.outrightWinner(Rank.parse("B++"), Rank.parse("EX")));
		assertNull(ClashRules.outrightWinner(Rank.parse("A"), Rank.parse("EX")));
		assertNull(ClashRules.outrightWinner(Rank.parse("A"), Rank.parse("A++")));
	}

	@Test
	void secondsLeft() {
		assertEquals(10.0, ClashRules.secondsLeft(1000, 100), EPS);
		assertEquals(0.0, ClashRules.secondsLeft(-5, 100), EPS);
		assertEquals(ENDLESS, ClashRules.secondsLeft(1000, 0));
	}

	@Test
	void meetingPointMovesTowardWhoeverRunsOutFirst() {
		assertEquals(0.5, ClashRules.meetingPoint(10, 10), EPS);
		assertEquals(0.25, ClashRules.meetingPoint(5, 15), EPS);
		assertEquals(0.0, ClashRules.meetingPoint(0, 15), EPS);
		assertEquals(1.0, ClashRules.meetingPoint(15, 0), EPS);
		assertEquals(0.5, ClashRules.meetingPoint(0, 0), EPS);
		assertEquals(1.0, ClashRules.meetingPoint(ENDLESS, 3), EPS);
		assertEquals(0.0, ClashRules.meetingPoint(3, ENDLESS), EPS);
		assertEquals(0.5, ClashRules.meetingPoint(ENDLESS, ENDLESS), EPS);
	}

	@Test
	void closeRanksTheLowerPaysOneAndAHalfTimesItsOwnDrain() {
		ClashConfig clash = ClashConfig.DEFAULTS;
		assertEquals(157.5, clash.drainPerSecond(105, Rank.parse("A"), Rank.parse("EX")), EPS);
		assertEquals(116.6, clash.drainPerSecond(116.6, Rank.parse("EX"), Rank.parse("A")), EPS);
		// same letter: both pay their own drain
		assertEquals(105.0, clash.drainPerSecond(105, Rank.parse("A"), Rank.parse("A++")), EPS);
		assertEquals(105.0, clash.drainPerSecond(105, Rank.parse("A++"), Rank.parse("A")), EPS);
	}

	@Test
	void loserTakesNinetyPercent() {
		assertEquals(315.0, ClashConfig.DEFAULTS.loserDamage(350), EPS);
	}
}
