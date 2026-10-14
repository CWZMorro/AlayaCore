package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilitySettings;
import io.github.cwzmorro.alayacore.ability.Presets;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.network.ChooseServantPayload;
import io.github.cwzmorro.alayacore.network.OpenServantSelectionPayload;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import io.github.cwzmorro.alayacore.progression.Grand;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.progression.TrueName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Handles server-side servant selection, keeping first-join players frozen and invulnerable until they answer.
 */
public final class ServantSelection {
	private static final AttributeModifier FROZEN = new AttributeModifier(
		AlayaCore.id("choosing_servant"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final List<Holder<Attribute>> FROZEN_ATTRIBUTES = List.of(Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.GRAVITY);
	// A frozen player pushed further than this from where they were frozen is put back (squared blocks).
	private static final double FROZEN_DRIFT_SQUARED = 1.0E-6;

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
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(ServantSelection::holdStill));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			!(entity instanceof ServerPlayer player && isUndecided(player)));
	}

	/**
	 * Opens the selection screen; the next answer from this player will be accepted. A player who has
	 * chosen before (Spirit Origin Changer) may close it without a change; the first-join screen must be answered.
	 */
	public static void open(ServerPlayer player) {
		player.setAttached(ServantStorage.CHOOSING, true);
		ServerPlayNetworking.send(player, new OpenServantSelectionPayload(
			player.level().getGameRules().get(AlayaGameRules.FORCE_RANDOM_SERVANT),
			AlayaConfig.servants().allowDuplicates(),
			!isUndecided(player),
			AlayaConfig.progression().demi(),
			realStats(player.level().registryAccess())));
	}

	public static Map<Identifier, ServantStats> realStats(RegistryAccess registries) {
		Map<Identifier, ServantStats> realStats = new HashMap<>();
		ServantClasses.ordered(registries).forEach((id, servantClass) -> realStats.put(id, AlayaConfig.servants().classConfig(id, servantClass).real()));
		return realStats;
	}

	public static Map<Identifier, String> rankLabels() {
		Map<Identifier, String> labels = new HashMap<>();
		for (Ability ability : AlayaRegistries.ABILITY) {
			String label = ability.rankLabel();
			if (label != null) {
				labels.put(AlayaRegistries.ABILITY.getKey(ability), label);
			}
		}
		return Map.copyOf(labels);
	}

	public static void choose(ServerPlayer player, ChooseServantPayload payload) {
		if (!player.getAttachedOrElse(ServantStorage.CHOOSING, false)) {
			return;
		}
		boolean forceRandom = player.level().getGameRules().get(AlayaGameRules.FORCE_RANDOM_SERVANT);
		List<Identifier> free = ServantRules.free(ServantDefinitions.selectable(player.level().registryAccess()).keySet(), owners(player.level().getServer()),
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
				if (forceRandom || payload.servant().filter(free::contains).isEmpty()) {
					open(player);
					return;
				}
				apply(player, payload.servant());
			}
		}
	}

	private static void apply(ServerPlayer player, Optional<Identifier> servant) {
		// The old servant's abilities stop before the new servant takes over, and its presets and settings go.
		Abilities.endAll(player);
		Presets.clear(player);
		AbilitySettings.clear(player);
		TrueName.onLeftServant(player);
		if (!AlayaServants.classId(player).equals(servant.flatMap(AlayaRegistries.SERVANT::getOptional).map(s -> s.servantClass().identifier()))) {
			Grand.loseTitle(player);
		}
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

	// Other mods can set the choice directly, so freezing must follow stored state rather than the screen.
	private static void holdStill(ServerPlayer player) {
		Vec3 at = player.getAttached(ServantStorage.FROZEN_AT);
		if (at == null) {
			return;
		}
		if (!isUndecided(player)) {
			setFrozen(player, false);
			return;
		}
		player.setDeltaMovement(Vec3.ZERO);
		if (player.position().distanceToSqr(at) > FROZEN_DRIFT_SQUARED) {
			player.connection.teleport(at.x, at.y, at.z, player.getYRot(), player.getXRot());
		}
	}

	private static void setFrozen(ServerPlayer player, boolean frozen) {
		if (frozen) {
			player.setAttached(ServantStorage.FROZEN_AT, player.position());
		} else {
			player.removeAttached(ServantStorage.FROZEN_AT);
		}
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
