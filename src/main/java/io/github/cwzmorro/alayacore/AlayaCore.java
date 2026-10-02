package io.github.cwzmorro.alayacore;

import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.AbilityStorage;
import io.github.cwzmorro.alayacore.ability.Presets;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.config.ConfigCommand;
import io.github.cwzmorro.alayacore.divinity.Divinity;
import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.mana.ManaCommand;
import io.github.cwzmorro.alayacore.mana.ManaEvents;
import io.github.cwzmorro.alayacore.mana.ManaStorage;
import io.github.cwzmorro.alayacore.progression.Grand;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.progression.TrueName;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import io.github.cwzmorro.alayacore.shield.Shields;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint. Runs on both the dedicated server and the client's integrated server. */
public final class AlayaCore implements ModInitializer {
	public static final String MOD_ID = "alayacore";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	/**
	 * A phase of {@link ServerPlayerEvents#AFTER_RESPAWN} after the default one, in which Fabric copies
	 * attachments to the new player. Handlers that read those attachments register here.
	 */
	public static final Identifier AFTER_ATTACHMENTS_COPIED = id("after_attachments_copied");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		AlayaRegistries.init();
		ServantClasses.init();
		AlayaGameRules.init();
		AlayaItems.init();
		ManaStorage.init();
		ServantStorage.init();
		AbilityStorage.init();
		Attacks.init();
		ServerPlayerEvents.AFTER_RESPAWN.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_ATTACHMENTS_COPIED);
		ManaEvents.register();
		ServantSelection.register();
		TrueName.register();
		Divinity.register();
		Shields.register();
		Progression.register();
		Grand.register();
		Abilities.register();
		Presets.register();
		ConfigCommand.register();
		ManaCommand.register();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> AlayaConfig.load());
		LOGGER.info("Alaya Core loaded");
	}
}
