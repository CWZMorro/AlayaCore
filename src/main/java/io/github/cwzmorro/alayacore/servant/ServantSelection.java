package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.Presets;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.network.ChooseServantPayload;
import io.github.cwzmorro.alayacore.network.OpenServantSelectionPayload;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.progression.TrueName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The servant selection flow on the server (plan 3): the first-join screen, the Spirit Origin
 * Changer, and checking and applying the answer.
 *
 * <p>A player who has not answered the first-join screen yet is invulnerable and frozen.
 */
public final class ServantSelection {
	private static final AttributeModifier FROZEN = new AttributeModifier(
		AlayaCore.id("choosing_servant"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final List<Holder<Attribute>> FROZEN_ATTRIBUTES = List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH);

	private ServantSelection() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(OpenServantSelectionPayload.TYPE, OpenServantSelectionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChooseServantPayload.TYPE, ChooseServantPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ChooseServantPayload.TYPE, (payload, context) -> choose(context.player(), payload));

		ServerPlayerEvents.JOIN.register(player -> {
			if (isUndecided(player)) {
				setFrozen(player, true);
				open(player);
			}
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(entity instanceof ServerPlayer player && isUndecided(player)));
	}

	/**
	 * Opens the selection screen; the next answer from this player will be accepted. A player who has
	 * decided before (Spirit Origin Changer) may close it without a change; the first-join screen must be answered.
	 */
	public static void open(ServerPlayer player) {
		player.setAttached(ServantStorage.CHOOSING, true);
		ServerPlayNetworking.send(player, new OpenServantSelectionPayload(
			player.level().getGameRules().get(AlayaGameRules.FORCE_RANDOM_SERVANT),
			AlayaConfig.servants().allowDuplicates(),
			!isUndecided(player)));
	}

	/** Handles an answer from the screen; ignored unless the screen was opened for this player. */
	public static void choose(ServerPlayer player, ChooseServantPayload payload) {
		if (!player.getAttachedOrElse(ServantStorage.CHOOSING, false)) {
			return;
		}
		boolean forceRandom = player.level().getGameRules().get(AlayaGameRules.FORCE_RANDOM_SERVANT);
		List<Identifier> free = ServantRules.free(AlayaRegistries.SERVANT.keySet(), owners(player.level().getServer()),
			player.getUUID(), AlayaConfig.servants().allowDuplicates());

		switch (payload.kind()) {
			case CANCEL -> {
				if (isUndecided(player)) {
					open(player);
				} else {
					player.removeAttached(ServantStorage.CHOOSING);
				}
			}
			case HUMAN -> apply(player, Optional.empty());
			case RANDOM -> {
				if (free.isEmpty()) {
					open(player);
					return;
				}
				Identifier servant = free.get(player.getRandom().nextInt(free.size()));
				apply(player, Optional.of(servant));
				player.sendSystemMessage(Component.translatable("alayacore.servant.random_result", Servant.name(servant)));
			}
			case SERVANT -> {
				// Taken meanwhile, unknown, or not allowed under force random: ask again.
				if (forceRandom || payload.servant().filter(free::contains).isEmpty()) {
					open(player);
					return;
				}
				apply(player, payload.servant());
			}
		}
	}

	private static void apply(ServerPlayer player, Optional<Identifier> servant) {
		// The old servant's abilities stop before the new servant takes over, and its presets go (plan 8).
		Abilities.endAll(player);
		Presets.clear(player);
		MinecraftServer server = player.level().getServer();
		Map<UUID, Identifier> owners = new HashMap<>(owners(server));
		servant.ifPresentOrElse(id -> owners.put(player.getUUID(), id), () -> owners.remove(player.getUUID()));
		server.globalAttachments().setAttached(ServantStorage.OWNERS, Map.copyOf(owners));

		player.setAttached(ServantStorage.CHOICE, servant.map(ServantChoice::servant).orElseGet(ServantChoice::human));
		player.removeAttached(ServantStorage.CHOOSING);
		setFrozen(player, false);
		if (servant.isPresent()) {
			TrueName.onBecameServant(player);
			ClassGrades.onBecameServant(player);
		}
		Progression.refresh(player);
		servant.ifPresent(id -> AlayaServants.BECAME_SERVANT.invoker().onBecameServant(player, id));
	}

	private static Map<UUID, Identifier> owners(MinecraftServer server) {
		return server.globalAttachments().getAttachedOrCreate(ServantStorage.OWNERS);
	}

	private static boolean isUndecided(ServerPlayer player) {
		return !player.getAttachedOrElse(ServantStorage.CHOICE, ServantChoice.UNDECIDED).decided();
	}

	private static void setFrozen(ServerPlayer player, boolean frozen) {
		for (Holder<Attribute> attribute : FROZEN_ATTRIBUTES) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance == null) {
				continue;
			}
			instance.removeModifier(FROZEN.id());
			if (frozen) {
				instance.addTransientModifier(FROZEN);
			}
		}
	}
}
