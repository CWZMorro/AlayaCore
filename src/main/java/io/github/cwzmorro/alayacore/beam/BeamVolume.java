package io.github.cwzmorro.alayacore.beam;

import io.github.cwzmorro.alayacore.combat.WeaponBlocks;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import io.github.cwzmorro.alayacore.shield.Shields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Held-beam containment, segmented entity lookup and no-drop carving; nothing behind a shield is hit or carved. */
public final class BeamVolume {
	// Blocks of beam each entity lookup covers: a long beam is looked up in segments, not in one huge box.
	private static final double LOOKUP_STEP = 16;
	private static final double CARVE_STEP = 1;

	private BeamVolume() {
	}

	/**
	 * Returns whether the point is inside a beam with unit direction and reach in blocks.
	 * The radius function receives distance along the beam.
	 */
	public static boolean contains(Vec3 origin, Vec3 direction, double reach, DoubleUnaryOperator radius, Vec3 point) {
		Vec3 offset = point.subtract(origin);
		double along = offset.dot(direction);
		return along >= 0 && along <= reach && offset.subtract(direction.scale(along)).length() <= radius.applyAsDouble(along);
	}

	/**
	 * Hurts every entity in the beam but {@code owner} and shields, once each, with {@code source} for {@code damage}, unless a
	 * shield stands between the beam's origin and it ({@link Shields#hurt}); {@code beam} is the entity that hits (never hit itself).
	 */
	public static void hit(ServerLevel level, Entity beam, LivingEntity owner, DamageSource source, float damage,
		Vec3 origin, Vec3 direction, double reach, DoubleUnaryOperator radius) {
		Set<Entity> hit = new HashSet<>();
		for (double from = 0; from < reach; from += LOOKUP_STEP) {
			double to = Math.min(reach, from + LOOKUP_STEP);
			AABB box = new AABB(origin.add(direction.scale(from)), origin.add(direction.scale(to)))
				.inflate(Math.max(radius.applyAsDouble(from), radius.applyAsDouble(to)));
			for (Entity target : level.getEntities(beam, box, target -> target != owner && !(target instanceof ShieldEntity)
				&& contains(origin, direction, reach, radius, target.getBoundingBox().getCenter()))) {
				if (hit.add(target)) {
					Shields.hurt(level, target, source, damage);
				}
			}
		}
	}

	public static void carve(ServerLevel level, Vec3 origin, Vec3 direction, double reach, DoubleUnaryOperator radius) {
		Set<BlockPos> inside = new HashSet<>();
		for (double along = 0; along <= reach; along += CARVE_STEP) {
			Vec3 centre = origin.add(direction.scale(along));
			int r = Mth.ceil(radius.applyAsDouble(along));
			BlockPos middle = BlockPos.containing(centre);
			for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-r, -r, -r), middle.offset(r, r, r))) {
				if (contains(origin, direction, reach, radius, Vec3.atCenterOf(pos))) {
					inside.add(pos.immutable());
				}
			}
		}
		for (BlockPos pos : Shields.unshielded(level, origin, new ArrayList<>(inside))) {
			WeaponBlocks.breakWithoutDrops(level, pos);
		}
	}
}
