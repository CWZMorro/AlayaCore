package io.github.cwzmorro.alayacore.mana;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

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
		assertEquals(0.035, DEFAULTS.foodFraction(2, COOKIE, 3.0), EPS); // 0.5% + 3%
		assertEquals(0.055, DEFAULTS.foodFraction(2, CAKE, 5.0), EPS); // per slice: 0.5% + 5%
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
		assertEquals(500, DEFAULTS.challengeBonus(1000), EPS); // How Did We Get Here?
		assertEquals(50, DEFAULTS.challengeBonus(100), EPS);
		assertEquals(42.5, DEFAULTS.challengeBonus(85), EPS); // Arbalistic
		assertEquals(25, DEFAULTS.challengeBonus(40), EPS); // floor
		assertEquals(25, DEFAULTS.challengeBonus(0), EPS); // no XP reward
		assertEquals(500, DEFAULTS.challengeBonus(2_000), EPS); // cap
	}

	@Test
	void killsGiveATenthOfTheVictimsMaxHealth() {
		assertEquals(2, DEFAULTS.killMaxManaGain(20), EPS);
		assertEquals(80, DEFAULTS.killMaxManaGain(800), EPS);
	}

	@Test
	void maxManaStopsAtTwelveThousand() {
		assertEquals(12_000, DEFAULTS.maxCeiling(), EPS);
	}

	@Test
	void allVanillaChallengesGiveEighteenHundredFifty() {
		// XP rewards of the 25 vanilla 26.1.2 challenge advancements (0 = none)
		int[] xp = {1000, 500, 500, 150, 100, 100, 100, 100, 100, 100, 100, 100, 100, 85, 65, 50, 50, 50, 50, 50, 50, 50, 40, 0, 0};
		double total = 0;
		for (int x : xp) {
			total += DEFAULTS.challengeBonus(x);
		}
		assertEquals(1_850, total, EPS);
	}
}
