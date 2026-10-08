package io.github.cwzmorro.alayacore.shield;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A shield's shape: what an attack's path has to cross to reach anything behind or inside it. An attack
 * whose path enters the shape is blocked; one that starts behind or inside it (the holder's own) is not.
 */
public interface ShieldShape {
	/**
	 * Returns the segment's first incoming crossing, or null if the path is not blocked.
	 *
	 * @param centre the shield's position
	 * @param facing unit vector the shield faces (toward the attacks it stops)
	 */
	@Nullable Vec3 entry(Vec3 centre, Vec3 facing, Vec3 from, Vec3 to);

	double radius();

	AABB bounds(Vec3 centre, Vec3 facing);

	default Vec3 nearestPoint(Vec3 centre, Vec3 facing, Vec3 point) {
		Vec3 entry = this.entry(centre, facing, point, centre);
		return entry == null ? centre : entry;
	}

	/** A flat disc that stops attacks from the side it faces only. */
	record Disc(double radius) implements ShieldShape {
		private static final double THICKNESS = 0.2;

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

		@Override
		public AABB bounds(Vec3 centre, Vec3 facing) {
			// Along each axis the disc reaches its radius × the sine of that axis's angle to the facing.
			Vec3 reach = new Vec3(this.across(facing.x), this.across(facing.y), this.across(facing.z));
			return new AABB(centre.subtract(reach), centre.add(reach)).inflate(THICKNESS / 2);
		}

		@Override
		public Vec3 nearestPoint(Vec3 centre, Vec3 facing, Vec3 point) {
			Vec3 offset = point.subtract(centre);
			Vec3 across = offset.subtract(facing.scale(offset.dot(facing)));
			double length = across.length();
			return centre.add(length > this.radius ? across.scale(this.radius / length) : across);
		}

		private double across(double facingPart) {
			return this.radius * Math.sqrt(Math.max(0.0, 1.0 - facingPart * facingPart));
		}
	}

	/** A finite cone with a small tip disc at centre and an open base behind it; only incoming paths meet its skin. */
	record Cone(double length, double baseRadius, double tipRadius) implements ShieldShape {
		@Override
		public double radius() {
			return Math.hypot(this.length, this.baseRadius);
		}

		@Override
		public AABB bounds(Vec3 centre, Vec3 facing) {
			return new Disc(this.baseRadius).bounds(centre.subtract(facing.scale(this.length)), facing)
				.minmax(new Disc(this.tipRadius).bounds(centre, facing));
		}

		@Override
		public @Nullable Vec3 entry(Vec3 centre, Vec3 facing, Vec3 from, Vec3 to) {
			Vec3 start = from.subtract(centre);
			Vec3 path = to.subtract(from);
			double along = start.dot(facing);
			double travel = path.dot(facing);
			Vec3 tip = new Disc(this.tipRadius).entry(centre, facing, from, to);
			if (tip != null) {
				return tip;
			}
			double slope = (this.baseRadius - this.tipRadius) / this.length;
			Vec3 radialStart = start.subtract(facing.scale(along));
			Vec3 radialPath = path.subtract(facing.scale(travel));
			double width = this.tipRadius - slope * along;
			double a = radialPath.lengthSqr() - slope * slope * travel * travel;
			double b = 2 * (radialStart.dot(radialPath) + width * slope * travel);
			double c = radialStart.lengthSqr() - width * width;
			if (along <= 0 && along >= -this.length && c <= 0) {
				return null;
			}
			double discriminant = b * b - 4 * a * c;
			if (discriminant < 0) {
				return null;
			}
			if (a == 0) {
				return b == 0 ? null : this.crossing(from, path, along, travel, -c / b, b);
			}
			double root = Math.sqrt(discriminant);
			double first = (-b - root) / (2 * a);
			double second = (-b + root) / (2 * a);
			Vec3 hit = this.crossing(from, path, along, travel, first, 2 * a * first + b);
			return hit == null ? this.crossing(from, path, along, travel, second, 2 * a * second + b) : hit;
		}

		private @Nullable Vec3 crossing(Vec3 from, Vec3 path, double along, double travel, double t, double derivative) {
			double axial = along + travel * t;
			return t >= 0 && t <= 1 && axial <= 0 && axial >= -this.length && derivative <= 0
				? from.add(path.scale(t)) : null;
		}
	}

	/** A sphere around its centre that nothing enters from outside; facing doesn't matter. */
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
