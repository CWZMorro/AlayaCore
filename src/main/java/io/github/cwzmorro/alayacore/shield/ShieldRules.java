package io.github.cwzmorro.alayacore.shield;

import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankRules;
import org.jspecify.annotations.Nullable;

/**
 * A shield's HP under attack and repair. No game calls. HP, cast cost and the
 * repair rate per rank are in {@link io.github.cwzmorro.alayacore.rank.RankConfig}.
 */
public final class ShieldRules {
	private ShieldRules() {
	}

	/**
	 * Returns remaining HP, or zero when the attack overwhelms the shield's rank; an absolute defence is never overwhelmed.
	 *
	 * @param attack the attack's rank, or null for an attack without one (melee, arrows), which only deals damage
	 */
	public static double hpAfterHit(@Nullable Rank attack, Rank shield, boolean absoluteDefence, double hp, double damage) {
		if (attack != null && RankRules.overwhelms(attack, shield, absoluteDefence)) {
			return 0.0;
		}
		return Math.max(0.0, hp - damage);
	}

	/**
	 * Returns missing HP affordable at {@code rate} mana per HP, without a repair speed limit.
	 */
	public static double repairable(double hp, double maxHp, double mana, double rate) {
		double missing = Math.max(0.0, maxHp - hp);
		return rate > 0 ? Math.min(missing, Math.max(0.0, mana) / rate) : missing;
	}
}
