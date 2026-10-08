package io.github.cwzmorro.alayacore.config;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.util.Percent;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

/**
 * Loads client preferences from {@code config/alayacore-client.toml} at startup and saves them when the preset screen closes.
 */
public final class ClientConfig {
	public static final String FILE_NAME = AlayaCore.MOD_ID + "-client.toml";
	private static final String HEADER = """
		# Alaya Core client settings. Read when the game starts; the preset screen's settings tab writes them.

		""";
	private static final String PRESET_HUD = "preset_hud";
	private static final String OPACITY = "opacity_percent";
	private static final double DEFAULT_OPACITY_PERCENT = Percent.WHOLE;

	private static volatile float presetHudOpacity = (float) Percent.toFraction(DEFAULT_OPACITY_PERCENT);

	private ClientConfig() {
	}

	public static float presetHudOpacity() {
		return presetHudOpacity;
	}

	/** Clamps opacity to 0–1 without saving; {@link #save} persists the change. */
	public static void setPresetHudOpacity(float opacity) {
		presetHudOpacity = Mth.clamp(opacity, 0.0F, 1.0F);
	}

	public static List<String> load() {
		return ConfigFile.sync(path(), HEADER, true, ClientConfig::sync);
	}

	public static List<String> save() {
		return ConfigFile.sync(path(), HEADER, false, ClientConfig::sync);
	}

	private static void sync(ConfigSection root) {
		ConfigSection hud = root.section(PRESET_HUD, null);
		double percent = hud.number(OPACITY, "How opaque the preset HUD on the right is, in percent (0 hides it).",
			Math.round(Percent.fromFraction(presetHudOpacity)), 0.0, Percent.WHOLE);
		presetHudOpacity = (float) Percent.toFraction(percent);
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}
}
