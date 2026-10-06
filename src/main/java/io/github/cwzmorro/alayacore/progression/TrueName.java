package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Tracks true-name deeds and awakening through advancements in the shared "Throne of Heroes" tab.
 * Grants progress directly because vanilla triggers cannot distinguish the player's servant.
 */
public final class TrueName {
	public static final ResourceKey<Registry<TrueNameDeeds>> DEEDS_KEY = ResourceKey.createRegistryKey(AlayaCore.id("true_name"));
	// The bosses that awaken the true name once every deed is done (the Ender Dragon and the Wither).
	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, AlayaCore.id("true_name_bosses"));
	public static final Identifier THRONE_OF_HEROES = AlayaCore.id("throne_of_heroes");

	private TrueName() {
	}

	public static void register() {
		DynamicRegistries.register(DEEDS_KEY, TrueNameDeeds.CODEC);
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, killed, source) -> {
			if (killer instanceof ServerPlayer player) {
				onKill(player, killed);
			}
		});
	}

	public static void onBecameServant(ServerPlayer player) {
		GrantedAdvancements.grant(player, THRONE_OF_HEROES);
		deeds(player).ifPresent(deeds -> GrantedAdvancements.grant(player, deeds.branch()));
	}

	public static boolean isRealized(ServerPlayer player) {
		return deeds(player).map(deeds -> GrantedAdvancements.isDone(player, deeds.awakening())).orElse(false);
	}

	public static void realize(ServerPlayer player) {
		deeds(player).ifPresent(deeds -> {
			deeds.deeds().forEach(deed -> GrantedAdvancements.grant(player, deed.advancement()));
			GrantedAdvancements.grant(player, deeds.awakening());
		});
		Progression.refresh(player);
	}

	public static void unrealize(ServerPlayer player) {
		deeds(player).ifPresent(deeds -> GrantedAdvancements.revoke(player, deeds.awakening()));
		Progression.refresh(player);
	}

	public static void onRaidWon(ServerPlayer player) {
		deeds(player).flatMap(deeds -> nextDeed(player, deeds))
			.filter(TrueNameDeeds.Deed::winRaid)
			.ifPresent(deed -> GrantedAdvancements.grant(player, deed.advancement()));
	}

	private static void onKill(ServerPlayer player, LivingEntity killed) {
		Optional<TrueNameDeeds> found = deeds(player);
		if (found.isEmpty()) {
			return;
		}
		TrueNameDeeds deeds = found.get();
		Identifier killedType = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
		nextDeed(player, deeds)
			.filter(deed -> deed.kill().filter(killedType::equals).isPresent())
			.ifPresent(deed -> GrantedAdvancements.grant(player, deed.advancement()));
		if (killed.is(BOSSES) && !GrantedAdvancements.isDone(player, deeds.awakening()) && nextDeed(player, deeds).isEmpty()) {
			GrantedAdvancements.grant(player, deeds.awakening());
			Progression.refresh(player);
		}
	}

	private static Optional<TrueNameDeeds.Deed> nextDeed(ServerPlayer player, TrueNameDeeds deeds) {
		return deeds.deeds().stream().filter(deed -> !GrantedAdvancements.isDone(player, deed.advancement())).findFirst();
	}

	private static Optional<TrueNameDeeds> deeds(ServerPlayer player) {
		return AlayaServants.servantId(player).flatMap(servant -> player.level().getServer().registryAccess()
			.lookup(DEEDS_KEY).flatMap(registry -> registry.getOptional(servant)));
	}
}
