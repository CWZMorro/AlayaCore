package io.github.cwzmorro.alayacore.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityPresets;
import io.github.cwzmorro.alayacore.ability.AbilityStorage;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.client.screen.PresetScreen;
import io.github.cwzmorro.alayacore.network.AbilityKeyPayload;
import io.github.cwzmorro.alayacore.network.SelectPresetPayload;
import io.github.cwzmorro.alayacore.network.ShiftSlotModePayload;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The ability keys (plan 8), Tensura's defaults: slots Z / X / C, Left Alt = next mode (held with a
 * slot key) and preset change (held with the scroll wheel or 1–5), previous mode unbound, the preset
 * screen on B, and one unbound quick-cast key per ability mode.
 */
public final class AbilityKeys {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AlayaCore.id("abilities"));
	/** Quick-cast keys are named {@code <prefix><ability namespace>.<ability path>.<mode>}; see {@link #quickCastName}. */
	private static final String QUICK_CAST_PREFIX = "key.alayacore.quick_cast.";

	private static final List<KeyMapping> SLOTS = List.of(
		new KeyMapping("key.alayacore.slot_1", InputConstants.KEY_Z, CATEGORY),
		new KeyMapping("key.alayacore.slot_2", InputConstants.KEY_X, CATEGORY),
		new KeyMapping("key.alayacore.slot_3", InputConstants.KEY_C, CATEGORY));
	private static final KeyMapping NEXT_MODE = new KeyMapping("key.alayacore.next_mode", InputConstants.KEY_LALT, CATEGORY);
	private static final KeyMapping PREVIOUS_MODE = new KeyMapping("key.alayacore.previous_mode", InputConstants.UNKNOWN.getValue(), CATEGORY);
	private static final KeyMapping PRESET_SCREEN = new KeyMapping("key.alayacore.presets", InputConstants.KEY_B, CATEGORY);

	private record QuickCast(KeyMapping key, Identifier ability, int mode) {
	}

	/** By key name, in registry order. */
	private static final Map<String, QuickCast> QUICK_CASTS = new LinkedHashMap<>();
	/** Keys down right now and the ability each one pressed, so the release goes to the same ability. */
	private static final Map<KeyMapping, Identifier> HELD = new HashMap<>();

	private AbilityKeys() {
	}

	/** Registers the keys; quick-cast keys for every ability registered so far (all of them, at client start). */
	public static void register() {
		SLOTS.forEach(KeyMappingHelper::registerKeyMapping);
		KeyMappingHelper.registerKeyMapping(NEXT_MODE);
		KeyMappingHelper.registerKeyMapping(PREVIOUS_MODE);
		KeyMappingHelper.registerKeyMapping(PRESET_SCREEN);
		for (Ability ability : AlayaRegistries.ABILITY) {
			Identifier id = AlayaRegistries.ABILITY.getKey(ability);
			for (int mode = 0; mode < ability.modes(); mode++) {
				String name = QUICK_CAST_PREFIX + id.getNamespace() + "." + id.getPath() + "." + mode;
				QUICK_CASTS.put(name, new QuickCast(KeyMappingHelper.registerKeyMapping(
					new KeyMapping(name, InputConstants.UNKNOWN.getValue(), CATEGORY)), id, mode));
			}
		}
		// Before vanilla handles its keys in the same tick, so 1–5 with the mode key don't also pick a hotbar slot.
		ClientTickEvents.START_CLIENT_TICK.register(AbilityKeys::tick);
	}

	/** The name shown in Controls for a quick-cast key: the ability, and its mode if it has several. */
	public static Optional<Component> quickCastName(String key) {
		return Optional.ofNullable(QUICK_CASTS.get(key)).map(quickCast -> {
			Component name = Ability.name(quickCast.ability);
			return Ability.modes(quickCast.ability) > 1
				? Component.translatable("key.alayacore.quick_cast", name, Ability.modeName(quickCast.ability, quickCast.mode))
				: name;
		});
	}

	/** The quick-cast key of one ability mode (empty for abilities registered after client start). */
	public static Optional<KeyMapping> quickCastKey(Identifier ability, int mode) {
		return QUICK_CASTS.values().stream().filter(q -> q.ability.equals(ability) && q.mode == mode).map(QuickCast::key).findFirst();
	}

	/** The key a preset slot is used with, as bound in Controls (e.g. "Z"). */
	public static Component slotKeyName(int slot) {
		return SLOTS.get(slot).getTranslatedKeyMessage();
	}

	/** The preset screen closes with its own key, like the inventory with E. */
	public static boolean isPresetScreenKey(KeyEvent event) {
		return PRESET_SCREEN.matches(event);
	}

	/** Mode key held + scroll: changes the preset instead of the hotbar slot. Returns whether it was used. */
	public static boolean onScroll(double delta) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !isModeKeyDown() || delta == 0.0) {
			return false;
		}
		// Like the hotbar: scrolling up goes back.
		selectPreset(presets(player).active() + (delta > 0 ? -1 : 1));
		return true;
	}

	private static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			HELD.clear();
			return;
		}
		AbilityPresets presets = presets(player);
		boolean modeKey = isModeKeyDown();

		for (int slot = 0; slot < SLOTS.size(); slot++) {
			KeyMapping key = SLOTS.get(slot);
			while (key.consumeClick()) {
				if (modeKey) {
					ClientPlayNetworking.send(new ShiftSlotModePayload(presets.active(), slot, PREVIOUS_MODE.isDown() && !NEXT_MODE.isDown() ? -1 : 1));
				} else {
					presets.slot(presets.active(), slot).ifPresent(filled -> press(player, key, filled.ability(), filled.mode()));
				}
			}
		}
		for (QuickCast quickCast : QUICK_CASTS.values()) {
			while (quickCast.key.consumeClick()) {
				press(player, quickCast.key, quickCast.ability, quickCast.mode);
			}
		}
		HELD.entrySet().removeIf(held -> {
			if (held.getKey().isDown()) {
				return false;
			}
			ClientPlayNetworking.send(new AbilityKeyPayload(held.getValue(), 0, false, false));
			return true;
		});

		if (modeKey) {
			for (int preset = 0; preset < AbilityPresets.PRESETS; preset++) {
				while (minecraft.options.keyHotbarSlots[preset].consumeClick()) {
					selectPreset(preset);
				}
			}
		}
		while (PRESET_SCREEN.consumeClick()) {
			minecraft.setScreen(new PresetScreen());
		}
	}

	private static void press(LocalPlayer player, KeyMapping key, Identifier ability, int mode) {
		if (HELD.containsKey(key)) {
			return;
		}
		HELD.put(key, ability);
		ClientPlayNetworking.send(new AbilityKeyPayload(ability, mode, true, player.isShiftKeyDown()));
	}

	private static void selectPreset(int preset) {
		ClientPlayNetworking.send(new SelectPresetPayload(Math.floorMod(preset, AbilityPresets.PRESETS)));
	}

	private static boolean isModeKeyDown() {
		return NEXT_MODE.isDown() || PREVIOUS_MODE.isDown();
	}

	private static AbilityPresets presets(LocalPlayer player) {
		return player.getAttachedOrElse(AbilityStorage.PRESETS, AbilityPresets.EMPTY);
	}
}
