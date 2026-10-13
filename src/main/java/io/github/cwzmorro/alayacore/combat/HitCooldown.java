package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.mixin.LivingEntityAccessor;
import io.github.cwzmorro.alayacore.servant.Servant;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hits that land through vanilla's hit cooldown without starting a new one: {@link ContinuousSource continuous damage}
 * and melee hits of a servant with {@link Servant#meleeIgnoresHitCooldown()}. The target's {@code invulnerableTime}
 * and {@code lastHurt} are put back after the hit.
 */
public final class HitCooldown {
	// Vanilla's hit cooldown after a full hit, in ticks. While more than half of it is left, a hit only adds what it exceeds the last.
	private static final int FULL_TICKS = 20;

	private HitCooldown() {
	}

	public static boolean ignoring(LivingEntity target, BooleanSupplier hit) {
		return within(target, 0, hit);
	}

	/**
	 * Applies a continuous tick while preserving the hit cooldown, suppressing repeated sounds and flashes until hurt time ends.
	 */
	public static boolean continuous(LivingEntity target, BooleanSupplier hit) {
		return within(target, target.hurtTime > 0 ? FULL_TICKS : 0, hit);
	}

	public static boolean melee(LivingEntity attacker, Entity target, BooleanSupplier hit) {
		if (target instanceof LivingEntity living && AlayaServants.servant(attacker).map(Servant::meleeIgnoresHitCooldown).orElse(false)) {
			return ignoring(living, hit);
		}
		return hit.getAsBoolean();
	}

	private static boolean within(LivingEntity target, int cooldown, BooleanSupplier hit) {
		LivingEntityAccessor accessor = (LivingEntityAccessor) target;
		int cooldownBefore = target.invulnerableTime;
		float lastHurtBefore = accessor.alayacore$lastHurt();
		target.invulnerableTime = cooldown;
		accessor.alayacore$setLastHurt(0.0F);
		try {
			return hit.getAsBoolean();
		} finally {
			target.invulnerableTime = cooldownBefore;
			accessor.alayacore$setLastHurt(lastHurtBefore);
		}
	}
}
