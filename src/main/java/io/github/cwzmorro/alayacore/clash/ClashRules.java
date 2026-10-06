package io.github.cwzmorro.alayacore.clash;

import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankRules;
import org.jspecify.annotations.Nullable;

/**
 * Two attacks meeting. No game calls; the attacks' mana drains come from the caller.
 *
 * <p>Far apart in rank, the higher wins at once. Otherwise both sides pay mana ({@link ClashConfig#drainPerSecond})
 * until one stops or runs out, and the meeting point slides toward the side that runs out first. Burst NPs never clash
 * ({@link AttackData#canClash}).
 */
public final class ClashRules {
	/** The two sides of a clash: A is the attack the meeting point is measured from, B the other. */
	public enum Side {
		A,
		B
	}

	private static final double AT_A = 0.0;
	private static final double AT_B = 1.0;
	private static final double EVEN = 0.5;

	private ClashRules() {
	}

	public static boolean clash(AttackData a, AttackData b) {
		return a.canClash() && b.canClash();
	}

	/** Returns the higher-ranked side if the rank gap wins outright, or null for a mana contest. */
	public static @Nullable Side outrightWinner(Rank a, Rank b) {
		if (RankRules.overwhelms(a, b)) {
			return Side.A;
		}
		return RankRules.overwhelms(b, a) ? Side.B : null;
	}

	public static double secondsLeft(double mana, double drainPerSecond) {
		return drainPerSecond > 0 ? Math.max(0.0, mana) / drainPerSecond : Double.POSITIVE_INFINITY;
	}

	/**
	 * Returns the meeting fraction from A (0) to B (1), moving toward the side with less casting time left.
	 */
	public static double meetingPoint(double secondsLeftA, double secondsLeftB) {
		boolean aEndless = Double.isInfinite(secondsLeftA);
		boolean bEndless = Double.isInfinite(secondsLeftB);
		if (aEndless && bEndless) {
			return EVEN;
		}
		if (aEndless || bEndless) {
			return aEndless ? AT_B : AT_A;
		}
		double total = secondsLeftA + secondsLeftB;
		return total > 0 ? secondsLeftA / total : EVEN;
	}
}
