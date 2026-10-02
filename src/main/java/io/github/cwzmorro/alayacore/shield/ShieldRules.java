package io.github.cwzmorro.alayacore.shield;

import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankRules;
import org.jspecify.annotations.Nullable;

/**
 * A shield's HP under attack and repair (plan 9, 13.7). Plain Java, no game calls. HP, cast cost and the
 * repair rate per rank are in {@link io.github.cwzmorro.alayacore.rank.RankConfig}.
 */
public final class ShieldRules {
	private ShieldRules() {
	}

	/**
	 * HP left after a hit. An attack far above the shield's rank breaks it at once, whatever HP it has left
	 * (never an absolute defence such as Avalon); every other attack takes its damage off, down to 0 (broken).
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
	 * HP the holder repairs now with {@code mana} at {@code rate} mana per HP: all that is missing, or as much as
	 * the mana pays for. No speed limit. A shield nobody holds gets no mana, so it never repairs.
	 */
	public static double repairable(double hp, double maxHp, double mana, double rate) {
		double missing = Math.max(0.0, maxHp - hp);
		return rate > 0 ? Math.min(missing, Math.max(0.0, mana) / rate) : missing;
	}
}
