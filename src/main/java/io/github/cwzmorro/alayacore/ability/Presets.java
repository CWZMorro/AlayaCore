package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.network.SelectPresetPayload;
import io.github.cwzmorro.alayacore.network.SetPresetSlotPayload;
import io.github.cwzmorro.alayacore.network.ShiftSlotModePayload;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Changing a player's presets on the server. Every request from a client is checked first. */
public final class Presets {
	private Presets() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SelectPresetPayload.TYPE, SelectPresetPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SetPresetSlotPayload.TYPE, SetPresetSlotPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ShiftSlotModePayload.TYPE, ShiftSlotModePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SelectPresetPayload.TYPE, (payload, context) -> select(context.player(), payload.preset()));
		ServerPlayNetworking.registerGlobalReceiver(SetPresetSlotPayload.TYPE,
			(payload, context) -> setSlot(context.player(), payload.preset(), payload.slot(), payload.ability()));
		ServerPlayNetworking.registerGlobalReceiver(ShiftSlotModePayload.TYPE,
			(payload, context) -> shiftMode(context.player(), payload.preset(), payload.slot(), payload.delta()));
	}

	public static AbilityPresets get(ServerPlayer player) {
		return player.getAttachedOrElse(AbilityStorage.PRESETS, AbilityPresets.EMPTY);
	}

	public static void select(ServerPlayer player, int preset) {
		if (AbilityPresets.isValid(preset, 0)) {
			player.setAttached(AbilityStorage.PRESETS, get(player).withActive(preset));
		}
	}

	public static void setSlot(ServerPlayer player, int preset, int slot, Optional<Identifier> ability) {
		if (AbilityPresets.isValid(preset, slot) && ability.map(id -> Abilities.owns(player, id) && Ability.listed(id)).orElse(true)) {
			player.setAttached(AbilityStorage.PRESETS, get(player).with(preset, slot, ability));
		}
	}

	public static void shiftMode(ServerPlayer player, int preset, int slot, int delta) {
		if (!AbilityPresets.isValid(preset, slot) || Math.abs(delta) != 1) {
			return;
		}
		AbilityPresets presets = get(player);
		presets.slot(preset, slot)
			.flatMap(filled -> AlayaRegistries.ABILITY.getOptional(filled.ability()))
			.ifPresent(ability -> player.setAttached(AbilityStorage.PRESETS, presets.withModeShifted(preset, slot, delta, ability.modes())));
	}

	public static void clear(ServerPlayer player) {
		player.removeAttached(AbilityStorage.PRESETS);
	}
}
