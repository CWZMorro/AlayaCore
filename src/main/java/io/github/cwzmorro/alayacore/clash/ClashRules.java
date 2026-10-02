package io.github.cwzmorro.alayacore.clash;

import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankRules;
import org.jspecify.annotations.Nullable;

/**
 * Two attacks meeting (plan 9). Plain Java, no game calls; the attacks' mana drains come from the caller.
 *
 * <p>Far apart, the higher rank wins at once. Otherwise both sides pay mana until one stops or runs out
 * (each its own attack's drain, the lower letter 1.5×: {@link ClashConfig#drainPerSecond}), and the meeting
 * point slides toward the side that would run out first. Burst NPs never clash ({@link AttackData#canClash}).
 */
public final class ClashRules {
	/** The two sides of a clash: A is the attack the meeting point is measured from, B the other. */
	public enum Side {
		A,
		B
	}

	/** Meeting point positions: at A's side, at B's side, and halfway (neither ever runs out, or both run out together). */
	private static final double AT_A = 0.0;
	private static final double AT_B = 1.0;
	private static final double EVEN = 0.5;

	private ClashRules() {
	}

	/** Any two attacks that both can clash do, whatever kind of attack they are. */
	public static boolean clash(AttackData a, AttackData b) {
		return a.canClash() && b.canClash();
	}

	/** The side that wins at once (far apart, higher rank), or null when they fight it out with mana. */
	public static @Nullable Side outrightWinner(Rank a, Rank b) {
		if (RankRules.overwhelms(a, b)) {
			return Side.A;
		}
		return RankRules.overwhelms(b, a) ? Side.B : null;
	}

	/** Seconds until a side runs out of mana at its current drain; infinite if it drains nothing. */
	public static double secondsLeft(double mana, double drainPerSecond) {
		return drainPerSecond > 0 ? Math.max(0.0, mana) / drainPerSecond : Double.POSITIVE_INFINITY;
	}

	/**
	 * Where the meeting point sits, from A's side (0) to B's (1): A's time left over both sides' time left.
	 * It moves toward whoever would run out first and reaches them as their mana runs out.
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
