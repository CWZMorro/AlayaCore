package io.github.cwzmorro.alayacore.util;

import java.math.BigDecimal;
import java.util.Locale;

/** Numbers as the text shows them. */
public final class Numbers {
	private Numbers() {
	}

	public static String tenths(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	public static String plain(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
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
