package io.github.cwzmorro.alayacore.shield;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Adds solid shield shapes to entity collisions on both sides and pushes overlapping bodies clear.
 */
public final class SolidShields {
	private static final int PUSH_PASSES = 4;

	private SolidShields() {
	}

	/** Returns vanilla collisions plus shield boxes solid to the mover. */
	public static List<VoxelShape> withShields(Entity mover, AABB area, List<VoxelShape> collisions) {
		List<ShieldEntity> shields = solidTo(mover, area);
		if (shields.isEmpty()) {
			return collisions;
		}
		List<VoxelShape> all = new ArrayList<>(collisions);
		for (ShieldEntity shield : shields) {
			for (AABB box : boxes(shield, area)) {
				all.add(Shapes.create(box));
			}
		}
		return all;
	}

	/**
	 * Returns whether the proposed body box overlaps a solid shield, for movement outside vanilla collision handling.
	 */
	public static boolean collides(Entity body, AABB box) {
		AABB inside = box.deflate(Shapes.EPSILON);
		for (ShieldEntity shield : solidTo(body, box)) {
			for (AABB cell : boxes(shield, box)) {
				if (cell.intersects(inside)) {
					return true;
				}
			}
		}
		return false;
	}

	private static List<ShieldEntity> solidTo(Entity body, AABB area) {
		// A shield's box can reach farther from its position than entity lookups look past an area.
		return body.level().getEntitiesOfClass(ShieldEntity.class, area.inflate(Shields.MAX_RADIUS),
			shield -> shield.isAlive() && shield.blocks(body));
	}

	// Push only where the body is simulated: server for mobs/items, local client for its player.
	static void pushOut(ShieldEntity shield) {
		if (!(shield.shape() instanceof ShieldShape.Solid solid)) {
			return;
		}
		for (Entity body : shield.level().getEntities(shield, shield.getBoundingBox(),
			body -> body.isLocalInstanceAuthoritative() && !body.noPhysics && shield.blocks(body))) {
			AABB box = body.getBoundingBox();
			Vec3 away = solid.outward(shield.position(), shield.getLookAngle(), box.getCenter());
			double push = 0.0;
			for (int pass = 0; pass < PUSH_PASSES; pass++) {
				AABB moved = box.move(away.scale(push)).deflate(Shapes.EPSILON);
				double needed = push;
				for (AABB cell : solid.boxes(shield.position(), shield.getLookAngle(), moved)) {
					if (cell.intersects(moved)) {
						needed = Math.max(needed, clearance(cell, box, away));
					}
				}
				if (needed == push) {
					break;
				}
				push = needed;
			}
			if (push > 0) {
				body.move(MoverType.SHULKER, away.scale(push));
			}
		}
	}

	private static List<AABB> boxes(ShieldEntity shield, AABB area) {
		return shield.shape() instanceof ShieldShape.Solid solid ? solid.boxes(shield.position(), shield.getLookAngle(), area) : List.of();
	}

	private static double clearance(AABB cell, AABB box, Vec3 away) {
		double least = Double.POSITIVE_INFINITY;
		for (Direction.Axis axis : Direction.Axis.values()) {
			double share = away.get(axis);
			if (Math.abs(share) > Shapes.EPSILON) {
				least = Math.min(least, share > 0 ? (cell.max(axis) - box.min(axis)) / share : (cell.min(axis) - box.max(axis)) / share);
			}
		}
		return least;
	}
}
