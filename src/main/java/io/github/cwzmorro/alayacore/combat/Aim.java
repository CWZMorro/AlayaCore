package io.github.cwzmorro.alayacore.combat;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a caster aims at: the living thing under or near their crosshair, else the block they look at. Each
 * ability passes its own range, from its own config.
 */
public final class Aim {
	private static final double PICK_MARGIN = 1.0;

	private Aim() {
	}

	/** Returns a target's centre, the looked-at block, or the range endpoint if neither is found. */
	public static Vec3 point(LivingEntity caster, double range) {
		return point(caster, caster.getLookAngle(), range);
	}

	public static Vec3 point(LivingEntity caster, Vec3 direction, double range) {
		Vec3 end = block(caster, direction, range).getLocation();
		LivingEntity target = targetAlong(caster, end);
		return target != null ? target.getBoundingBox().getCenter() : end;
	}

	/** Returns the nearest living target intersecting the crosshair before a block, or null if none is found. */
	public static @Nullable LivingEntity target(LivingEntity caster, double range) {
		return targetAlong(caster, block(caster, caster.getLookAngle(), range).getLocation());
	}

	private static @Nullable LivingEntity targetAlong(LivingEntity caster, Vec3 end) {
		Vec3 eye = caster.getEyePosition();
		LivingEntity nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Entity entity : caster.level().getEntities(caster, new AABB(eye, end).inflate(PICK_MARGIN),
			entity -> entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator())) {
			Optional<Vec3> hit = entity.getBoundingBox().inflate(PICK_MARGIN).clip(eye, end);
			if (hit.isPresent()) {
				double distance = hit.get().distanceToSqr(eye);
				if (distance < nearestDistance) {
					nearest = (LivingEntity) entity;
					nearestDistance = distance;
				}
			}
		}
		return nearest;
	}

	private static BlockHitResult block(LivingEntity caster, Vec3 direction, double range) {
		Vec3 eye = caster.getEyePosition();
		return caster.level().clip(new ClipContext(eye, eye.add(direction.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
			caster));
	}
}
