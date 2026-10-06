package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.network.AbilityKeyPayload;
import io.github.cwzmorro.alayacore.progression.TrueName;
import io.github.cwzmorro.alayacore.servant.Servant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * Manages server-side casts for living entities. Player input arrives through {@link AbilityKeyPayload};
 * mobs can call {@link #press} and {@link #release} directly.
 */
public final class Abilities {
	/** Items whose held use is a cast: content mods tag their channeled weapon abilities. */
	public static final TagKey<Item> CHANNELED = TagKey.create(Registries.ITEM, AlayaCore.id("channeled"));

	private Abilities() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(AbilityKeyPayload.TYPE, AbilityKeyPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AbilityKeyPayload.TYPE, (payload, context) -> {
			if (payload.pressed()) {
				press(context.player(), payload.ability(), payload.mode(), payload.sneaking());
			} else {
				release(context.player(), payload.ability());
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> endAll(entity));
		// Also covers logging out and changing dimension.
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof LivingEntity living) {
				endAll(living);
			}
		});
	}

	public static List<ResourceKey<Ability>> owned(LivingEntity entity) {
		return AlayaServants.servant(entity).map(Servant::abilities).orElse(List.of());
	}

	/**
	 * Starts a cast or notifies an existing cast of another press.
	 * Ignores unknown or unowned abilities and invalid modes.
	 */
	public static void press(LivingEntity entity, Identifier abilityId, int mode, boolean sneaking) {
		Ability ability = AlayaRegistries.ABILITY.getValue(abilityId);
		if (ability == null || mode < 0 || mode >= ability.modes()
			|| !owned(entity).contains(ResourceKey.create(AlayaRegistries.ABILITY_KEY, abilityId))) {
			return;
		}
		Map<Identifier, AbilityCast> casts = casts(entity);
		AbilityCast active = casts.get(abilityId);
		if (active != null) {
			active.press(sneaking);
			ability.onPressedAgain(active);
			settle(entity, active);
			return;
		}

		long now = entity.level().getGameTime();
		double cost = ability.startCost(entity, mode);
		Optional<Refused> refused = refusal(entity, abilityId, ability, mode, casts, cost);
		if (refused.isPresent()) {
			refuse(entity, refused.get().reason(), refused.get().secondsLeft());
			return;
		}
		Component cannotStart = ability.cannotStart(entity, mode);
		if (cannotStart != null) {
			if (entity instanceof ServerPlayer player) {
				player.sendOverlayMessage(cannotStart);
			}
			return;
		}
		AbilityCast cast = new AbilityCast(entity, abilityId, ability, mode, sneaking, now);
		entity.getAttachedOrCreate(AbilityStorage.CASTS).put(abilityId, cast);
		if (cost > 0.0) {
			AlayaMana.addCurrent(entity, -cost);
		}
		ability.onStart(cast);
		settle(entity, cast);
	}

	public static void release(LivingEntity entity, Identifier abilityId) {
		AbilityCast cast = casts(entity).get(abilityId);
		if (cast == null || !cast.held()) {
			return;
		}
		cast.release();
		if (cast.holding()) {
			cast.ability().onReleased(cast);
		} else {
			cast.ability().onTap(cast);
		}
		settle(entity, cast);
	}

	/** Runs every active cast for one tick. Called from the entity's own tick on the server. */
	public static void tick(LivingEntity entity) {
		int threshold = AlayaConfig.abilities().holdThresholdTicks();
		for (AbilityCast cast : new ArrayList<>(casts(entity).values())) {
			Ability ability = cast.ability();
			if (cast.held()) {
				if (cast.tickHeld(threshold)) {
					ability.onHoldStart(cast);
				}
				if (cast.holding() && !cast.ended()) {
					ability.onHeldTick(cast);
				}
			}
			if (!cast.ended()) {
				ability.onTick(cast);
			}
			settle(entity, cast);
		}
	}

	public static void endAll(LivingEntity entity) {
		for (AbilityCast cast : new ArrayList<>(casts(entity).values())) {
			cast.end();
			settle(entity, cast);
		}
	}

	public static boolean isActive(LivingEntity entity, Identifier abilityId) {
		return casts(entity).containsKey(abilityId);
	}

	/** Returns whether the entity has an active cast, which can pause mana regeneration. */
	public static boolean isCasting(LivingEntity entity) {
		return !casts(entity).isEmpty();
	}

	/** Returns whether the entity is using a {@link #CHANNELED} item, which counts as casting. */
	public static boolean isChanneling(LivingEntity entity) {
		return entity.isUsingItem() && entity.getUseItem().is(CHANNELED);
	}

	static void refuse(LivingEntity entity, AbilityRules.Refusal reason, double secondsLeft) {
		if (entity instanceof ServerPlayer player) {
			player.sendOverlayMessage(reason == AbilityRules.Refusal.ON_COOLDOWN
				? Component.translatable(reason.translationKey(), String.format(Locale.ROOT, "%.1f", secondsLeft))
				: Component.translatable(reason.translationKey()));
		}
	}

	private record Refused(AbilityRules.Refusal reason, double secondsLeft) {
	}

	private static Optional<Refused> refusal(LivingEntity entity, Identifier abilityId, Ability ability, int mode,
		Map<Identifier, AbilityCast> casts, double cost) {
		if (ability.noblePhantasm() && entity instanceof ServerPlayer player && !TrueName.isRealized(player)) {
			return Optional.of(new Refused(AbilityRules.Refusal.NEEDS_TRUE_NAME, 0.0));
		}
		boolean exclusiveActive = casts.values().stream().anyMatch(cast -> cast.ability().exclusive());
		if (AbilityRules.isBlocked(ability.exclusive(), !casts.isEmpty(), exclusiveActive)) {
			return Optional.of(new Refused(AbilityRules.Refusal.BLOCKED, 0.0));
		}
		double secondsLeft = cooldownSecondsLeft(entity, abilityId, mode);
		if (secondsLeft > 0.0) {
			return Optional.of(new Refused(AbilityRules.Refusal.ON_COOLDOWN, secondsLeft));
		}
		ManaData mana = AlayaMana.get(entity);
		if (cost > 0.0 && (mana == null || !AbilityRules.canAfford(mana.current(), cost))) {
			return Optional.of(new Refused(AbilityRules.Refusal.NOT_ENOUGH_MANA, 0.0));
		}
		return Optional.empty();
	}

	private static void settle(LivingEntity entity, AbilityCast cast) {
		if (!cast.ended()) {
			return;
		}
		Map<Identifier, AbilityCast> casts = casts(entity);
		if (casts.get(cast.abilityId()) != cast) {
			return;
		}
		casts.remove(cast.abilityId());
		if (casts.isEmpty()) {
			entity.removeAttached(AbilityStorage.CASTS);
		}
		cast.ability().onEnd(cast);
		double seconds = AlayaConfig.abilities().cooldownSeconds(cast.abilityId(), cast.ability(), cast.mode());
		if (seconds > 0.0) {
			startCooldown(entity, cast, entity.level().getGameTime() + AbilityRules.toTicks(seconds));
		}
	}

	private static void startCooldown(LivingEntity entity, AbilityCast cast, long endTick) {
		Map<Identifier, List<Long>> cooldowns = new HashMap<>(entity.getAttachedOrCreate(AbilityStorage.COOLDOWNS));
		List<Long> saved = cooldowns.get(cast.abilityId());
		List<Long> perMode = new ArrayList<>(saved != null && saved.size() == cast.ability().modes()
			? saved
			: Collections.nCopies(cast.ability().modes(), 0L));
		perMode.set(cast.mode(), endTick);
		cooldowns.put(cast.abilityId(), List.copyOf(perMode));
		entity.setAttached(AbilityStorage.COOLDOWNS, Map.copyOf(cooldowns));
	}

	public static double cooldownSecondsLeft(LivingEntity entity, Identifier abilityId, int mode) {
		List<Long> perMode = entity.getAttachedOrElse(AbilityStorage.COOLDOWNS, Map.<Identifier, List<Long>>of()).get(abilityId);
		return perMode == null || mode >= perMode.size() ? 0.0 : AbilityRules.secondsLeft(perMode.get(mode), entity.level().getGameTime());
	}

	private static Map<Identifier, AbilityCast> casts(LivingEntity entity) {
		return entity.getAttachedOrElse(AbilityStorage.CASTS, Collections.emptyMap());
	}
}
