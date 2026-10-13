package io.github.cwzmorro.alayacore.util;

/** Numbers as the text shows them. */
public final class Numbers {
	private Numbers() {
	}

	public static Object shown(Object value) {
		if (value instanceof Number number && !(value instanceof Long) && !(value instanceof Integer)) {
			double d = number.doubleValue();
			if (d == Math.rint(d) && !Double.isInfinite(d)) {
				return (long) d;
			}
		}
		return value;
	}
}
