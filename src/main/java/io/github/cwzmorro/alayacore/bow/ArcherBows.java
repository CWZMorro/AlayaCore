package io.github.cwzmorro.alayacore.bow;

import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.progression.TrueName;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;

/** Archer draw and ordinary arrow speed, scaled by the progression bar, for any servant of the Archer class. */
public final class ArcherBows {
	// Vanilla's fully drawn bow projectile speed: the literal pow * 3.0F in BowItem.releaseUsing, not exposed as a constant.
	public static final float VANILLA_SPEED = 3.0F;

	private ArcherBows() {
	}

	public static boolean archer(LivingEntity entity) {
		return AlayaServants.classId(entity).filter(ServantClasses.ARCHER.identifier()::equals).isPresent();
	}

	public static double bar(LivingEntity entity) {
		return archer(entity) ? AlayaConfig.progression().barFill(Progression.sync(entity),
			entity instanceof ServerPlayer player && TrueName.isRealized(player)) : 0.0;
	}

	public static double drawTicks(double bar) {
		return BowItem.MAX_DRAW_DURATION - AlayaConfig.archerBows().archerDrawReduction() * bar;
	}

	public static float power(LivingEntity entity, int heldTicks) {
		return powerFraction((float) (heldTicks / drawTicks(bar(entity))));
	}

	public static float powerFraction(float fraction) {
		return Math.min(1.0F, (fraction * fraction + fraction * 2.0F) / 3.0F);
	}

	public static float speedMultiplier(double bar) {
		return (float) (1.0 + AlayaConfig.archerBows().archerSpeedBonus() * bar);
	}

	/** Returns an Archer's class attack bonus, excluding equipment and potion melee bonuses. */
	public static double attack(LivingEntity entity) {
		if (!archer(entity)) {
			return 0.0;
		}
		return AlayaServants.classConfig(entity).map(config -> {
			double demi = AlayaConfig.progression().demi().attack();
			return demi + (config.real().attack() - demi) * bar(entity);
		}).orElse(0.0);
	}
}
