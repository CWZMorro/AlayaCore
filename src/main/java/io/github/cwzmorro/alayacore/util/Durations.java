package io.github.cwzmorro.alayacore.util;

import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;

/** Lengths of time as the player reads them, and configured ones as server ticks. */
public final class Durations {
	private static final int SECONDS_PER_MINUTE = 60;

	private Durations() {
	}

	public static int ticks(double seconds) {
		return Math.max(1, (int) Math.round(seconds * SharedConstants.TICKS_PER_SECOND));
	}

	public static String tenths(double seconds) {
		return String.format(Locale.ROOT, "%.1f", seconds);
	}

	public static Component text(int seconds) {
		return seconds % SECONDS_PER_MINUTE == 0
			? Component.translatable("alayacore.duration.minutes", seconds / SECONDS_PER_MINUTE)
			: Component.translatable("alayacore.duration.seconds", seconds);
	}
}
