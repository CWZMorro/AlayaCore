package io.github.cwzmorro.alayacore.shield;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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

	/**
	 * A shape a solid shield can have: boxes that bodies collide with as with blocks, and the way a body
	 * the shield is moved onto is pushed out.
	 */
	interface Solid extends ShieldShape {
		List<AABB> boxes(Vec3 centre, Vec3 facing, AABB area);

		/** Returns the outward unit push direction on the body's current side, never into the ground. */
		Vec3 outward(Vec3 centre, Vec3 facing, Vec3 point);
	}

	/** A flat disc that stops attacks from the side it faces only; solid, it is as thick as its box. */
	record Disc(double radius) implements Solid {
		private static final double THICKNESS = 0.2;
		// Solid cells the disc is cut into, in blocks, on the world grid: fine across it, but a whole block up it, so a leaning
		// disc makes steps of a block that nobody walks up (only jumps, as on blocks).
		private static final double CELL = 0.25;
		private static final double CELL_UP = 1.0;

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

		@Override
		public List<AABB> boxes(Vec3 centre, Vec3 facing, AABB area) {
			List<AABB> boxes = new ArrayList<>();
			if (!this.bounds(centre, facing).inflate(THICKNESS / 2).intersects(area)) {
				return boxes;
			}
			Direction.Axis depth = depthAxis(facing);
			Direction.Axis first = depth == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
			Direction.Axis second = depth == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
			double facingDepth = facing.get(depth);
			// The thickness, measured along the facing, spans this much along the depth axis; the disc this much in all.
			double halfDepth = THICKNESS / 2 / Math.abs(facingDepth);
			double depthReach = this.across(facingDepth) + THICKNESS / 2 * Math.abs(facingDepth);
			double firstReach = this.across(facing.get(first));
			double secondReach = this.across(facing.get(second));
			double firstCell = cell(first);
			double secondCell = cell(second);
			double firstFrom = Math.max(area.min(first), centre.get(first) - firstReach);
			double firstTo = Math.min(area.max(first), centre.get(first) + firstReach);
			double secondFrom = Math.max(area.min(second), centre.get(second) - secondReach);
			double secondTo = Math.min(area.max(second), centre.get(second) + secondReach);
			for (int i = Mth.floor(firstFrom / firstCell); i * firstCell < firstTo; i++) {
				double a0 = Math.max(i * firstCell, centre.get(first) - firstReach) - centre.get(first);
				double a1 = Math.min((i + 1) * firstCell, centre.get(first) + firstReach) - centre.get(first);
				for (int j = Mth.floor(secondFrom / secondCell); j * secondCell < secondTo; j++) {
					double b0 = Math.max(j * secondCell, centre.get(second) - secondReach) - centre.get(second);
					double b1 = Math.min((j + 1) * secondCell, centre.get(second) + secondReach) - centre.get(second);
					// The cell's point nearest the disc's centre must lie on the disc.
					double nearA = Math.clamp(0.0, a0, a1);
					double nearB = Math.clamp(0.0, b0, b1);
					double nearDepth = (facing.get(first) * nearA + facing.get(second) * nearB) / facingDepth;
					if (nearA * nearA + nearB * nearB + nearDepth * nearDepth > this.radius * this.radius) {
						continue;
					}
					double[] corners = {plane(facing, first, second, facingDepth, a0, b0), plane(facing, first, second, facingDepth, a0, b1),
						plane(facing, first, second, facingDepth, a1, b0), plane(facing, first, second, facingDepth, a1, b1)};
					double low = Math.max(Math.min(Math.min(corners[0], corners[1]), Math.min(corners[2], corners[3])) - halfDepth, -depthReach)
						+ centre.get(depth);
					double high = Math.min(Math.max(Math.max(corners[0], corners[1]), Math.max(corners[2], corners[3])) + halfDepth, depthReach)
						+ centre.get(depth);
					if (high <= area.min(depth) || low >= area.max(depth)) {
						continue;
					}
					boxes.add(new AABB(
						Vec3.ZERO.with(first, a0 + centre.get(first)).with(second, b0 + centre.get(second)).with(depth, low),
						Vec3.ZERO.with(first, a1 + centre.get(first)).with(second, b1 + centre.get(second)).with(depth, high)));
				}
			}
			return boxes;
		}

		@Override
		public Vec3 outward(Vec3 centre, Vec3 facing, Vec3 point) {
			Vec3 out = depthAxis(facing) == Direction.Axis.Y ? new Vec3(0, Math.signum(facing.y), 0) : new Vec3(facing.x, 0, facing.z).normalize();
			return point.subtract(centre).dot(facing) < 0 ? out.reverse() : out;
		}

		private double across(double facingPart) {
			return this.radius * Math.sqrt(Math.max(0.0, 1.0 - facingPart * facingPart));
		}

		private static Direction.Axis depthAxis(Vec3 facing) {
			Direction.Axis depth = Direction.Axis.Y;
			for (Direction.Axis axis : Direction.Axis.values()) {
				if (Math.abs(facing.get(axis)) > Math.abs(facing.get(depth))) {
					depth = axis;
				}
			}
			return depth;
		}

		private static double plane(Vec3 facing, Direction.Axis first, Direction.Axis second, double facingDepth, double a, double b) {
			return -(facing.get(first) * a + facing.get(second) * b) / facingDepth;
		}

		private static double cell(Direction.Axis axis) {
			return axis == Direction.Axis.Y ? CELL_UP : CELL;
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
