package io.github.cwzmorro.alayacore.shield;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A shield's shape (plan 9): what an attack's path has to cross to reach anything behind or inside it. An attack
 * whose path enters the shape is blocked; one that starts behind or inside it (the holder's own) is not.
 */
public interface ShieldShape {
	/**
	 * Where the path from {@code from} to {@code to} enters the shield, or null if it never does.
	 *
	 * @param centre the shield's position
	 * @param facing unit vector the shield faces (toward the attacks it stops)
	 */
	@Nullable Vec3 entry(Vec3 centre, Vec3 facing, Vec3 from, Vec3 to);

	/** How far the shape reaches from its centre, in blocks. */
	double radius();

	/** The box around it: what players aim at to hit it. */
	AABB bounds(Vec3 centre, Vec3 facing);

	/** A flat disc that stops attacks from the side it faces only (Rho Aias, Avalon). */
	record Disc(double radius) implements ShieldShape {
		/** How thick its box is, in blocks, so a disc seen edge-on can still be hit. */
		private static final double THICKNESS = 0.2;

		@Override
		public AABB bounds(Vec3 centre, Vec3 facing) {
			// Along each axis the disc reaches its radius × the sine of that axis's angle to the facing.
			Vec3 reach = new Vec3(this.across(facing.x), this.across(facing.y), this.across(facing.z));
			return new AABB(centre.subtract(reach), centre.add(reach)).inflate(THICKNESS / 2);
		}

		private double across(double facingPart) {
			return this.radius * Math.sqrt(Math.max(0.0, 1.0 - facingPart * facingPart));
		}

		@Override
		public @Nullable Vec3 entry(Vec3 centre, Vec3 facing, Vec3 from, Vec3 to) {
			double fromSide = from.subtract(centre).dot(facing);
			double toSide = to.subtract(centre).dot(facing);
			if (fromSide <= 0 || toSide > 0) {
				return null;
			}
			Vec3 crossing = from.lerp(to, fromSide / (fromSide - toSide));
			return crossing.distanceToSqr(centre) <= this.radius * this.radius ? crossing : null;
		}
	}

	/** A sphere around its centre that nothing enters from outside (Lord Camelot); facing doesn't matter. */
	record Dome(double radius) implements ShieldShape {
		@Override
		public AABB bounds(Vec3 centre, Vec3 facing) {
			return new AABB(centre, centre).inflate(this.radius);
		}

		@Override
		public @Nullable Vec3 entry(Vec3 centre, Vec3 facing, Vec3 from, Vec3 to) {
			Vec3 start = from.subtract(centre);
			Vec3 path = to.subtract(from);
			double radiusSqr = this.radius * this.radius;
			double a = path.lengthSqr();
			double c = start.lengthSqr() - radiusSqr;
			if (c <= 0 || a == 0) {
				return null;
			}
			// |start + t·path|² = r²: the first t in [0, 1] is where the path enters.
			double b = 2 * start.dot(path);
			double discriminant = b * b - 4 * a * c;
			if (discriminant < 0) {
				return null;
			}
			double t = (-b - Math.sqrt(discriminant)) / (2 * a);
			return t >= 0 && t <= 1 ? from.add(path.scale(t)) : null;
		}
	}
}
