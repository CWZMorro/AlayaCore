package io.github.cwzmorro.alayacore.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RankTest {
	@Test
	void parseAndPrint() {
		assertEquals(new Rank(RankLetter.B, 1), Rank.parse("B+"));
		assertEquals(new Rank(RankLetter.EX, 2), Rank.parse(" ex++ "));
		assertEquals(new Rank(RankLetter.E, 0), Rank.parse("E"));
		for (RankLetter letter : RankLetter.values()) {
			for (int p = 0; p <= Rank.MAX_PLUSES; p++) {
				Rank rank = new Rank(letter, p);
				assertEquals(rank, Rank.parse(rank.toString()));
			}
		}
	}

	@Test
	void parseRejectsBadText() {
		assertThrows(IllegalArgumentException.class, () -> Rank.parse("A+++"));
		assertThrows(IllegalArgumentException.class, () -> Rank.parse("F"));
		assertThrows(IllegalArgumentException.class, () -> Rank.parse("+"));
		assertThrows(IllegalArgumentException.class, () -> new Rank(RankLetter.A, 3));
	}

	@Test
	void ordering() {
		assertTrue(Rank.parse("A").compareTo(Rank.parse("A+")) < 0);
		assertTrue(Rank.parse("A++").compareTo(Rank.parse("EX")) < 0);
		assertTrue(Rank.parse("E++").compareTo(Rank.parse("D")) < 0);
		assertEquals(0, Rank.parse("B+").compareTo(Rank.parse("B+")));
	}

	@Test
	void gapCountsLettersOnly() {
		// plan 9 examples and QnA R3 #23
		assertEquals(RankGap.FAR, RankRules.gap(Rank.parse("EX"), Rank.parse("B")));
		assertEquals(RankGap.CLOSE, RankRules.gap(Rank.parse("A"), Rank.parse("B")));
		assertEquals(RankGap.CLOSE, RankRules.gap(Rank.parse("A"), Rank.parse("EX")));
		assertEquals(RankGap.FAR, RankRules.gap(Rank.parse("B++"), Rank.parse("EX")));
		assertEquals(RankGap.SAME, RankRules.gap(Rank.parse("A"), Rank.parse("A++")));
	}

	@Test
	void overwhelms() {
		assertTrue(RankRules.overwhelms(Rank.parse("EX"), Rank.parse("B")));
		assertFalse(RankRules.overwhelms(Rank.parse("B"), Rank.parse("EX")));
		assertFalse(RankRules.overwhelms(Rank.parse("EX"), Rank.parse("A")));
		assertFalse(RankRules.overwhelms(Rank.parse("A++"), Rank.parse("A")));
		// Avalon: EX but blocks everything
		assertFalse(RankRules.overwhelms(Rank.parse("EX++"), Rank.parse("C"), true));
		assertTrue(RankRules.overwhelms(Rank.parse("EX++"), Rank.parse("C"), false));
	}
}
