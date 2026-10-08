package io.github.cwzmorro.alayacore.client.hud;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityPresets;
import io.github.cwzmorro.alayacore.ability.AbilityStorage;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import io.github.cwzmorro.alayacore.config.ClientConfig;
import io.github.cwzmorro.alayacore.util.Durations;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public final class PresetHud {
	private static final int MARGIN = 4;
	private static final int KEY_TEXT_GAP = 4;
	private static final int ROW_HEIGHT = 20;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int SUBTEXT = 0xFFAAAAAA;
	private static final int COOLDOWN = 0xAA0000;
	public static final Component EMPTY_SLOT = Component.translatable("alayacore.hud.empty_slot");

	private PresetHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, AlayaCore.id("presets"), PresetHud::extract);
	}

	private record Row(Component key, Component name, @Nullable Component detail) {
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		float opacity = ClientConfig.presetHudOpacity();
		if (opacity <= 0.0F || player == null || player.isSpectator() || minecraft.getDebugOverlay().showDebugScreen() || Abilities.owned(player).isEmpty()) {
			return;
		}
		AbilityPresets presets = player.getAttachedOrElse(AbilityStorage.PRESETS, AbilityPresets.EMPTY);
		Font font = minecraft.font;
		List<Row> rows = new ArrayList<>();
		int textWidth = 0;
		for (int slot = 0; slot < AbilityPresets.SLOTS; slot++) {
			Optional<AbilityPresets.Slot> filled = presets.slot(presets.active(), slot);
			Row row = new Row(AbilityKeys.slotKeyName(slot),
				filled.map(f -> Ability.name(f.ability())).orElse(EMPTY_SLOT),
				filled.map(f -> detail(player, f.ability(), f.mode())).orElse(null));
			rows.add(row);
			textWidth = Math.max(textWidth, Math.max(font.width(row.name), row.detail == null ? 0 : font.width(row.detail)));
		}

		int width = KeyBox.SIZE + KEY_TEXT_GAP + textWidth;
		int x = graphics.guiWidth() - MARGIN - width;
		int rowY = (graphics.guiHeight() - rows.size() * ROW_HEIGHT) / 2;
		int textX = x + KeyBox.SIZE + KEY_TEXT_GAP;
		int nameColor = ARGB.multiplyAlpha(TEXT, opacity);
		int detailColor = ARGB.multiplyAlpha(SUBTEXT, opacity);
		for (Row row : rows) {
			KeyBox.extract(graphics, font, x, rowY, row.key, opacity);
			if (row.detail == null) {
				graphics.text(font, row.name, textX, KeyBox.centredTextY(font, rowY, KeyBox.SIZE), nameColor, true);
			} else {
				graphics.text(font, row.name, textX, rowY, nameColor, true);
				graphics.text(font, row.detail, textX, rowY + font.lineHeight + 1, detailColor, true);
			}
			rowY += ROW_HEIGHT;
		}
	}

	private static @Nullable Component detail(LocalPlayer player, Identifier ability, int mode) {
		boolean hasModes = Ability.modes(ability) > 1;
		double cooldown = Abilities.cooldownSecondsLeft(player, ability, mode);
		if (!hasModes && cooldown <= 0.0) {
			return null;
		}
		MutableComponent detail = Component.empty();
		if (hasModes) {
			detail.append(Ability.modeName(ability, mode));
		}
		if (cooldown > 0.0) {
			detail.append(hasModes ? " " : "")
				.append(Component.translatable("alayacore.hud.cooldown", Durations.tenths(cooldown)).withColor(COOLDOWN));
		}
		return detail;
	}
}
