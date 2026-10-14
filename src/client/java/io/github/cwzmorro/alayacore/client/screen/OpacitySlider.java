package io.github.cwzmorro.alayacore.client.screen;

import io.github.cwzmorro.alayacore.config.ClientConfig;
import io.github.cwzmorro.alayacore.util.Percent;
import net.minecraft.network.chat.Component;

final class OpacitySlider {
	private static final double STEP_PERCENT = 1.0;

	private OpacitySlider() {
	}

	static SettingSlider create(int x, int y, int width) {
		return new SettingSlider(x, y, width, 0.0, Percent.WHOLE, STEP_PERCENT, Math.round(Percent.fromFraction(ClientConfig.presetHudOpacity())),
			percent -> Component.translatable("screen.alayacore.presets.opacity", Math.round(percent)),
			percent -> ClientConfig.setPresetHudOpacity((float) Percent.toFraction(percent)));
	}
}
