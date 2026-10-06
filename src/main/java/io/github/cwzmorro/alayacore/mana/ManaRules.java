package io.github.cwzmorro.alayacore.mana;

/**
 * Changing mana values. No game calls. The amounts themselves come from
 * {@link ManaConfig}.
 */
public final class ManaRules {
	private ManaRules() {
	}

	public static ManaData addCurrent(ManaData mana, double amount) {
		return mana.withCurrent(Math.clamp(mana.current() + amount, 0.0, mana.max()));
	}

	/** Raises max mana; current stays as it is and fills by regeneration. */
	public static ManaData addMax(ManaData mana, double amount) {
		return mana.withMax(Math.max(0.0, mana.max() + amount));
	}

	public static double fraction(ManaData mana) {
		return mana.max() > 0.0 ? mana.current() / mana.max() : 0.0;
	}

	public static ManaData addFractionOfMax(ManaData mana, double fraction) {
		return addCurrent(mana, mana.max() * fraction);
	}
}
