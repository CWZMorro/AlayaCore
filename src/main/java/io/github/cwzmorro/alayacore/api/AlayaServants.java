package io.github.cwzmorro.alayacore.api;

import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantDefinitions;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.Optional;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Provides entity servant identities and configured class values. */
public final class AlayaServants {
	/** Fires on the server after a selection is applied; content mods may grant starting items here. */
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

	public static Optional<Identifier> servantId(LivingEntity entity) {
		return entity.getAttachedOrElse(ServantStorage.CHOICE, ServantChoice.UNDECIDED).servant();
	}

	public static Optional<Servant> servant(LivingEntity entity) {
		return servantId(entity).flatMap(id -> ServantDefinitions.get(entity.level().registryAccess(), id));
	}

	public static Optional<Identifier> classId(LivingEntity entity) {
		return servant(entity).map(servant -> servant.servantClass().identifier());
	}

	public static Optional<ServantClassConfig> classConfig(LivingEntity entity) {
		return servant(entity).flatMap(servant -> ServantClasses.get(entity.level().registryAccess(), servant.servantClass())
			.map(servantClass -> AlayaConfig.servants().classConfig(servant.servantClass().identifier(), servantClass)));
	}

	/**
	 * Returns the class regeneration multiplier, or 1 for entities without a class.
	 * Returns zero while casting or channeling unless the class permits regeneration.
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
