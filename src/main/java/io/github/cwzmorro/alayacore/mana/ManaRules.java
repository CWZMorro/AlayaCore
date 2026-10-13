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

	/**
	 * Raises max mana up to {@code ceiling}; current stays as it is and fills by regeneration.
	 * A max already above the ceiling (set by a command) is never lowered by a gain.
	 */
	public static ManaData addMax(ManaData mana, double amount, double ceiling) {
		double raised = Math.min(mana.max() + amount, Math.max(ceiling, mana.max()));
		return mana.withMax(Math.max(0.0, raised));
	}

	public static double fraction(ManaData mana) {
		return mana.max() > 0.0 ? mana.current() / mana.max() : 0.0;
	}

	public static ManaData addFractionOfMax(ManaData mana, double fraction) {
		return addCurrent(mana, mana.max() * fraction);
	}
}
