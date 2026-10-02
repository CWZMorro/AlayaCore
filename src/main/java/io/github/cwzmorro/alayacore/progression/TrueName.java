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
 * True name realization (plan 13.3). Deeds and the awakening are advancements in the shared
 * "Throne of Heroes" tab, granted here: vanilla's triggers can't tell which servant a player is.
 * Advancement completion is the record of what was done.
 */
public final class TrueName {
	public static final ResourceKey<Registry<TrueNameDeeds>> DEEDS_KEY = ResourceKey.createRegistryKey(AlayaCore.id("true_name"));
	/** The bosses that awaken the true name once every deed is done (plan 13.3: Ender Dragon, Wither). */
	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, AlayaCore.id("true_name_bosses"));
	/** Root of the shared tab, granted when a player first becomes a servant. */
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

	/** The player just became a servant: open the tab and the servant's branch. */
	public static void onBecameServant(ServerPlayer player) {
		GrantedAdvancements.grant(player, THRONE_OF_HEROES);
		deeds(player).ifPresent(deeds -> GrantedAdvancements.grant(player, deeds.branch()));
	}

	/** True if the player's current servant has realized its true name. */
	public static boolean isRealized(ServerPlayer player) {
		return deeds(player).map(deeds -> GrantedAdvancements.isDone(player, deeds.awakening())).orElse(false);
	}

	/** Realizes the player's true name at once: every deed of their servant done, and the awakening. */
	public static void realize(ServerPlayer player) {
		deeds(player).ifPresent(deeds -> {
			deeds.deeds().forEach(deed -> GrantedAdvancements.grant(player, deed.advancement()));
			GrantedAdvancements.grant(player, deeds.awakening());
		});
		Progression.refresh(player);
	}

	/** The player's servant loses its true name (the awakening; the deeds stay done), to be awakened again by a boss. */
	public static void unrealize(ServerPlayer player) {
		deeds(player).ifPresent(deeds -> GrantedAdvancements.revoke(player, deeds.awakening()));
		Progression.refresh(player);
	}

	/** The player just won a raid. */
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

	/** The first deed not done yet: deeds count only in order (plan 13.3). Empty once all are done. */
	private static Optional<TrueNameDeeds.Deed> nextDeed(ServerPlayer player, TrueNameDeeds deeds) {
		return deeds.deeds().stream().filter(deed -> !GrantedAdvancements.isDone(player, deed.advancement())).findFirst();
	}

	private static Optional<TrueNameDeeds> deeds(ServerPlayer player) {
		return AlayaServants.servantId(player).flatMap(servant -> player.level().getServer().registryAccess()
			.lookup(DEEDS_KEY).flatMap(registry -> registry.getOptional(servant)));
	}
}
