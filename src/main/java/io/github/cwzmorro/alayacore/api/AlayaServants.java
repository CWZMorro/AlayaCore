package io.github.cwzmorro.alayacore.api;

import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.Optional;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Which servant an entity is, and its class values. */
public final class AlayaServants {
	/**
	 * A player has just become a servant, on the server: chosen on the selection screen (by name or at random), after the
	 * choice is applied. Content mods give their servant's starting items here.
	 */
	public static final Event<BecameServant> BECAME_SERVANT = EventFactory.createArrayBacked(BecameServant.class, listeners -> (player, servant) -> {
		for (BecameServant listener : listeners) {
			listener.onBecameServant(player, servant);
		}
	});

	private AlayaServants() {
	}

	@FunctionalInterface
	public interface BecameServant {
		void onBecameServant(ServerPlayer player, Identifier servant);
	}

	/** The entity's servant id; empty for humans, undecided players and entities without one. */
	public static Optional<Identifier> servantId(LivingEntity entity) {
		return entity.getAttachedOrElse(ServantStorage.CHOICE, ServantChoice.UNDECIDED).servant();
	}

	public static Optional<Servant> servant(LivingEntity entity) {
		return servantId(entity).flatMap(AlayaRegistries.SERVANT::getOptional);
	}

	/** The entity's servant class id, if it is a servant. */
	public static Optional<Identifier> classId(LivingEntity entity) {
		return servant(entity).map(servant -> servant.servantClass().identifier());
	}

	/** The entity's class values from the config, if it is a servant. */
	public static Optional<ServantClassConfig> classConfig(LivingEntity entity) {
		return servant(entity).flatMap(servant -> AlayaRegistries.SERVANT_CLASS.getOptional(servant.servantClass())
			.map(servantClass -> AlayaConfig.servants().classConfig(servant.servantClass().identifier(), servantClass)));
	}

	/**
	 * Multiplier for every kind of mana regeneration (Archer: 1.2; 1 for anything else). 0 while casting
	 * or holding an ability (or channeling an item), unless the class keeps regenerating (plan 7).
	 */
	public static double manaRegenMultiplier(LivingEntity entity) {
		Optional<ServantClassConfig> config = classConfig(entity);
		boolean casting = Abilities.isCasting(entity) || Abilities.isChanneling(entity);
		if (casting && !config.map(ServantClassConfig::regenWhileCasting).orElse(false)) {
			return 0.0;
		}
		return config.map(ServantClassConfig::manaRegenMultiplier).orElse(1.0);
	}
}
