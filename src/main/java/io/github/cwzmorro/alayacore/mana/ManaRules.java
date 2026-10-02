package io.github.cwzmorro.alayacore.mana;

/**
 * Changing mana values. Plain Java, no game calls. The amounts themselves come from
 * {@link ManaConfig}.
 */
public final class ManaRules {
	private ManaRules() {
	}

	/** Adds (or with a negative amount removes) current mana, kept between 0 and max. */
	public static ManaData addCurrent(ManaData mana, double amount) {
		return mana.withCurrent(Math.clamp(mana.current() + amount, 0.0, mana.max()));
	}

	/** Raises max mana; current stays as it is and fills by regeneration. */
	public static ManaData addMax(ManaData mana, double amount) {
		return mana.withMax(Math.max(0.0, mana.max() + amount));
	}

	/** Current mana as a fraction of max (0–1), for display; 0 when max is 0. */
	public static double fraction(ManaData mana) {
		return mana.max() > 0.0 ? mana.current() / mana.max() : 0.0;
	}

	/** Adds a fraction of max to current mana (food, sleep). */
	public static ManaData addFractionOfMax(ManaData mana, double fraction) {
		return addCurrent(mana, mana.max() * fraction);
	}
}
