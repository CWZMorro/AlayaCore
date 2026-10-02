package io.github.cwzmorro.alayacore.ability;

import java.util.Locale;
import net.minecraft.SharedConstants;

/** When an ability may start, and tap vs hold (plan 8). Plain Java, no game calls. */
public final class AbilityRules {
	/** Why a press was refused; each has an action-bar message {@code alayacore.ability.refused.<name>}. */
	public enum Refusal {
		NEEDS_TRUE_NAME,
		BLOCKED,
		ON_COOLDOWN,
		NOT_ENOUGH_MANA;

		public String translationKey() {
			return "alayacore.ability.refused." + this.name().toLowerCase(Locale.ROOT);
		}
	}

	private AbilityRules() {
	}

	/**
	 * Exclusivity: nothing starts while an exclusive ability is active, and an exclusive ability
	 * doesn't start while any other is active.
	 */
	public static boolean isBlocked(boolean exclusive, boolean anyActive, boolean exclusiveActive) {
		return exclusiveActive || exclusive && anyActive;
	}

	public static boolean canAfford(double mana, double cost) {
		return mana >= cost;
	}

	/** Held this many ticks, the press is a hold rather than a tap. */
	public static boolean isHold(int heldTicks, int thresholdTicks) {
		return heldTicks >= thresholdTicks;
	}

	public static int toTicks(double seconds) {
		return (int) Math.round(seconds * SharedConstants.TICKS_PER_SECOND);
	}

	/** Seconds left until {@code endTick}, 0 once it has passed. */
	public static double secondsLeft(long endTick, long now) {
		return Math.max(0L, endTick - now) / (double) SharedConstants.TICKS_PER_SECOND;
	}
}
