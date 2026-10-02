package io.github.cwzmorro.alayacore.mana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** The defaults must be plan.md 7 and 13.2. */
class ManaConfigTest {
	private static final double EPS = 1e-9;
	private static final ManaConfig DEFAULTS = ManaConfig.DEFAULTS;
	private static final Identifier COOKIE = Identifier.withDefaultNamespace("cookie");
	private static final Identifier CAKE = Identifier.withDefaultNamespace("cake");
	private static final Identifier STEAK = Identifier.withDefaultNamespace("cooked_beef");

	@Test
	void startsAtOneThousand() {
		assertEquals(new ManaData(1000, 1000), DEFAULTS.start());
		assertEquals(3.0, DEFAULTS.regenPerSecond(), EPS);
	}

	@Test
	void foodByNutritionPlusSweets() {
		assertEquals(0.02, DEFAULTS.foodFraction(8, STEAK), EPS);
		assertEquals(0.035, DEFAULTS.foodFraction(2, COOKIE), EPS); // 0.5% + 3%
		assertEquals(0.055, DEFAULTS.foodFraction(2, CAKE), EPS); // per slice: 0.5% + 5%
	}

	@Test
	void sleepGivesTwentyPercent() {
		assertEquals(0.2, DEFAULTS.sleepFraction(), EPS);
	}

	@Test
	void respawnAtExactlyHalf() {
		assertEquals(new ManaData(5000, 10_000), DEFAULTS.afterRespawn(new ManaData(9000, 10_000)));
		assertEquals(new ManaData(5000, 10_000), DEFAULTS.afterRespawn(new ManaData(100, 10_000)));
	}

	@Test
	void challengeBonus() {
		assertEquals(5000, DEFAULTS.challengeBonus(1000), EPS); // How Did We Get Here?
		assertEquals(425, DEFAULTS.challengeBonus(85), EPS); // Arbalistic
		assertEquals(250, DEFAULTS.challengeBonus(40), EPS); // floor
		assertEquals(250, DEFAULTS.challengeBonus(0), EPS); // no XP reward
		assertEquals(5000, DEFAULTS.challengeBonus(100_000), EPS); // cap
	}

	@Test
	void allVanillaChallengesGiveEighteenThousandFiveHundred() {
		// XP rewards of the 25 vanilla 26.1.2 challenge advancements (0 = none)
		int[] xp = {1000, 500, 500, 150, 100, 100, 100, 100, 100, 100, 100, 100, 100, 85, 65, 50, 50, 50, 50, 50, 50, 50, 40, 0, 0};
		double total = 0;
		for (int x : xp) {
			total += DEFAULTS.challengeBonus(x);
		}
		assertEquals(18_500, total, EPS);
	}
}
