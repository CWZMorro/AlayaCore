package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.network.SetAbilitySettingPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Reading and changing what a player chose for their abilities' settings. Every request from a client is checked first. */
public final class AbilitySettings {
	private AbilitySettings() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(SetAbilitySettingPayload.TYPE, SetAbilitySettingPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SetAbilitySettingPayload.TYPE,
			(payload, context) -> set(context.player(), payload.ability(), payload.setting(), payload.value()));
	}

	public static double value(LivingEntity entity, Identifier ability, AbilitySetting setting) {
		return entity.getAttachedOrElse(AbilityStorage.SETTINGS, AbilitySettingValues.EMPTY).of(ability, setting);
	}

	public static void set(ServerPlayer player, Identifier ability, String setting, double value) {
		if (!Abilities.owns(player, ability)) {
			return;
		}
		AlayaRegistries.ABILITY.getOptional(ability).stream()
			.flatMap(registered -> registered.settings().stream())
			.filter(declared -> declared.id().equals(setting))
			.findFirst()
			.ifPresent(declared -> player.setAttached(AbilityStorage.SETTINGS,
				player.getAttachedOrElse(AbilityStorage.SETTINGS, AbilitySettingValues.EMPTY).with(ability, setting, declared.clamp(value))));
	}

	public static void clear(ServerPlayer player) {
		player.removeAttached(AbilityStorage.SETTINGS);
	}
}
