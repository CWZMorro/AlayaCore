package io.github.cwzmorro.alayacore.marble;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A marble's sphere: what lies inside it, and where a segment crosses its shell. */
public final class MarbleSphere {
	private MarbleSphere() {
	}

	public static boolean contains(Vec3 center, double radius, Vec3 point) {
		return point.distanceToSqr(center) <= radius * radius;
	}

	public static double entry(Vec3 center, double radius, Vec3 from, Vec3 to) {
		return crossing(center, radius, from, to, false);
	}

	private static double exit(Vec3 center, double radius, Vec3 from, Vec3 to) {
		return crossing(center, radius, from, to, true);
	}

	/**
	 * Returns the entry point, or exit point for a segment starting inside; uses the end if both ends are inside.
	 * Returns null if the segment misses.
	 */
	public static @Nullable Vec3 impact(Vec3 center, double radius, Vec3 from, Vec3 to) {
		double entry = entry(center, radius, from, to);
		if (Double.isNaN(entry)) {
			return null;
		}
		double leaving = entry == 0 ? exit(center, radius, from, to) : Double.NaN;
		return from.lerp(to, Double.isNaN(leaving) ? entry : leaving);
	}

	// Solves |offset + t * delta|^2 = radius^2 for the segment's fraction t, the nearer root entering, the farther leaving.
	private static double crossing(Vec3 center, double radius, Vec3 from, Vec3 to, boolean leaving) {
		Vec3 offset = from.subtract(center);
		double constant = offset.lengthSqr() - radius * radius;
		if (constant <= 0 && !leaving) {
			return 0;
		}
		if (constant > 0 && leaving) {
			return Double.NaN;
		}
		Vec3 delta = to.subtract(from);
		double quadratic = delta.lengthSqr();
		if (quadratic == 0) {
			return leaving ? 0 : Double.NaN;
		}
		double linear = offset.dot(delta);
		double discriminant = linear * linear - quadratic * constant;
		if (discriminant < 0) {
			return Double.NaN;
		}
		if (leaving) {
			return Math.min(1, (-linear + Math.sqrt(discriminant)) / quadratic);
		}
		double fraction = (-linear - Math.sqrt(discriminant)) / quadratic;
		return fraction >= 0 && fraction <= 1 ? fraction : Double.NaN;
	}
}
