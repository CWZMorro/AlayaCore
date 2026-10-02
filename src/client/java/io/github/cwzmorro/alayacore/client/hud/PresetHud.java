package io.github.cwzmorro.alayacore.client.hud;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityPresets;
import io.github.cwzmorro.alayacore.ability.AbilityStorage;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * The active preset (plan 8, 14), on the right edge at mid height: per slot, its key in a slot box, the ability, and its
 * mode and cooldown; no panel or title behind them, the text lit and shadowed as vanilla's HUD text is. Hidden for
 * players without abilities, in spectator, and with F1 / F3.
 */
public final class PresetHud {
	private static final Identifier KEY_SLOT = Identifier.withDefaultNamespace("container/slot");
	private static final int MARGIN = 4;
	private static final int KEY_SLOT_SIZE = 18;
	private static final int KEY_TEXT_GAP = 4;
	private static final int ROW_HEIGHT = 20;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int SUBTEXT = 0xFFAAAAAA;
	/** Text colour (RGB, no alpha) of the cooldown, used inside the grey detail line. */
	private static final int COOLDOWN = 0xAA0000;
	private static final int KEY_TEXT = 0xFFFFFFFF;

	private PresetHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, AlayaCore.id("presets"), PresetHud::extract);
	}

	/** One slot: its key, the ability (or "empty") and the line below it. */
	private record Row(Component key, Component name, Component detail) {
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || player.isSpectator() || minecraft.getDebugOverlay().showDebugScreen() || Abilities.owned(player).isEmpty()) {
			return;
		}
		AbilityPresets presets = player.getAttachedOrElse(AbilityStorage.PRESETS, AbilityPresets.EMPTY);
		Font font = minecraft.font;
		List<Row> rows = new ArrayList<>();
		int textWidth = 0;
		for (int slot = 0; slot < AbilityPresets.SLOTS; slot++) {
			Optional<AbilityPresets.Slot> filled = presets.slot(presets.active(), slot);
			Row row = new Row(AbilityKeys.slotKeyName(slot),
				filled.map(f -> Ability.name(f.ability())).orElse(Component.translatable("alayacore.hud.empty_slot")),
				filled.map(f -> detail(player, f.ability(), f.mode())).orElse(Component.empty()));
			rows.add(row);
			textWidth = Math.max(textWidth, Math.max(font.width(row.name), font.width(row.detail)));
		}

		int width = KEY_SLOT_SIZE + KEY_TEXT_GAP + textWidth;
		int x = graphics.guiWidth() - MARGIN - width;
		int rowY = (graphics.guiHeight() - rows.size() * ROW_HEIGHT) / 2;
		int textX = x + KEY_SLOT_SIZE + KEY_TEXT_GAP;
		for (Row row : rows) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, KEY_SLOT, x, rowY, KEY_SLOT_SIZE, KEY_SLOT_SIZE);
			graphics.centeredText(font, row.key, x + KEY_SLOT_SIZE / 2, rowY + (KEY_SLOT_SIZE - font.lineHeight) / 2 + 1, KEY_TEXT);
			if (row.detail.getString().isEmpty()) {
				graphics.text(font, row.name, textX, rowY + (KEY_SLOT_SIZE - font.lineHeight) / 2 + 1, TEXT, true);
			} else {
				graphics.text(font, row.name, textX, rowY, TEXT, true);
				graphics.text(font, row.detail, textX, rowY + font.lineHeight + 1, SUBTEXT, true);
			}
			rowY += ROW_HEIGHT;
		}
	}

	/** The ability's mode (if it has several) and the cooldown left, in red. */
	private static Component detail(LocalPlayer player, Identifier ability, int mode) {
		MutableComponent detail = Component.empty();
		if (Ability.modes(ability) > 1) {
			detail.append(Ability.modeName(ability, mode));
		}
		double cooldown = Abilities.cooldownSecondsLeft(player, ability, mode);
		if (cooldown > 0.0) {
			detail.append(detail.getSiblings().isEmpty() ? "" : " ")
				.append(Component.translatable("alayacore.hud.cooldown", String.format(Locale.ROOT, "%.1f", cooldown)).withColor(COOLDOWN));
		}
		return detail;
	}
}
