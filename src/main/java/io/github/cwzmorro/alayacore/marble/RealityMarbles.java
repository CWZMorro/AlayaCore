package io.github.cwzmorro.alayacore.marble;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.mana.ManaData;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Applies shared terrain, overlap and lifecycle rules to registered {@link RealityMarbleSource}s.
 */
public final class RealityMarbles {
	// The end-of-tick phase forced closing runs in, before the ordinary end-of-tick handlers: a marble whose caster is gone closes
	// before its own mod's upkeep runs that tick.
	private static final Identifier FORCED_CLOSING = AlayaCore.id("reality_marble_forced_closing");
	// The spawns of the world itself: the spawn cycle, a chunk's first generation, structures and patrols.
	// Commands, eggs, spawners, other mods' summons, breeding and conversions are none of these and still work.
	private static final Set<EntitySpawnReason> NATURAL_SPAWNS = EnumSet.of(EntitySpawnReason.NATURAL,
		EntitySpawnReason.CHUNK_GENERATION, EntitySpawnReason.STRUCTURE, EntitySpawnReason.PATROL);
	private static final List<RealityMarbleSource> SOURCES = new ArrayList<>();
	private static final Map<MinecraftServer, Set<UUID>> UNAVAILABLE = new WeakHashMap<>();

	private RealityMarbles() {
	}

	/** How a new marble fares against the open marbles it would overlap. */
	public enum Verdict {
		CLEAR,
		LOST,
		BROKEN,
		CLOSING
	}

	public record Overlap(Verdict verdict, List<RealityMarble> losers) { }

	public static void register(RealityMarbleSource source) {
		SOURCES.add(source);
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> !protects(world, pos));
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, loaded) ->
			!NATURAL_SPAWNS.contains(reason) || !shelters(level, entity.position()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity.level() instanceof ServerLevel level) {
				unavailable(entity, level.getServer());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> unavailable(handler.player, server));
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof LivingEntity living && entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
				unavailable(living, level.getServer());
			}
		});
		ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(FORCED_CLOSING, Event.DEFAULT_PHASE);
		ServerTickEvents.END_SERVER_TICK.register(FORCED_CLOSING, RealityMarbles::closeForced);
		ServerLifecycleEvents.SERVER_STOPPED.register(UNAVAILABLE::remove);
	}

	public static List<RealityMarble> marbles(MinecraftServer server) {
		List<RealityMarble> marbles = new ArrayList<>();
		for (RealityMarbleSource source : SOURCES) {
			marbles.addAll(source.marbles(server));
		}
		return marbles;
	}

	public static boolean intact(RealityMarble marble) {
		return marble.phase() == RealityMarble.Phase.OPENING || marble.phase() == RealityMarble.Phase.STANDING;
	}

	public static double strength(LivingEntity caster) {
		ManaData mana = AlayaMana.get(caster);
		return mana == null ? 0.0 : mana.current();
	}

	public static boolean protects(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		for (RealityMarbleSource source : SOURCES) {
			if (source.protects(serverLevel, pos)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether any position occupied by the resolved placement is protected marble terrain.
	 */
	public static boolean refusesPlacement(BlockPlaceContext context, BlockState state) {
		for (BlockPos pos : occupied(context.getClickedPos(), state)) {
			if (protects(context.getLevel(), pos)) {
				return true;
			}
		}
		return false;
	}

	// Every position a placement fills, from vanilla's two-block properties: its own, and the other half of a two-high block
	// (doors, tall plants) or of a bed, whatever mod's block carries them.
	private static List<BlockPos> occupied(BlockPos pos, BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return List.of(pos, state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
		}
		if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return List.of(pos, pos.relative(BedBlock.getConnectedDirection(state)));
		}
		return List.of(pos);
	}

	public static @Nullable RealityMarble at(ServerLevel level, Vec3 point) {
		for (RealityMarble marble : marbles(level.getServer())) {
			if (marble.phase() != RealityMarble.Phase.CLOSING && marble.dimension().equals(level.dimension()) && marble.contains(point)) {
				return marble;
			}
		}
		return null;
	}

	public static boolean shelters(ServerLevel level, Vec3 point) {
		return at(level, point) != null;
	}

	/**
	 * Evaluates overlap without closing incumbents, using caster mana before the opening cost.
	 * Requires greater strength than each intact incumbent; broken or closing marbles block opening.
	 */
	public static Overlap overlap(ServerLevel level, LivingEntity caster, Vec3 center, double radius) {
		List<RealityMarble> overlapping = new ArrayList<>();
		for (RealityMarble incumbent : marbles(level.getServer())) {
			double radii = incumbent.radius() + radius;
			if (incumbent.dimension().equals(level.dimension()) && center.distanceToSqr(incumbent.center()) <= radii * radii) {
				overlapping.add(incumbent);
			}
		}
		double strength = strength(caster);
		for (RealityMarble incumbent : overlapping) {
			if (incumbent.phase() == RealityMarble.Phase.BROKEN) {
				return new Overlap(Verdict.BROKEN, List.of());
			}
			if (incumbent.phase() == RealityMarble.Phase.CLOSING) {
				return new Overlap(Verdict.CLOSING, List.of());
			}
			if (!(strength > incumbent.strength())) {
				return new Overlap(Verdict.LOST, List.of());
			}
		}
		return new Overlap(Verdict.CLEAR, List.copyOf(overlapping));
	}

	/**
	 * Shatters intact marbles crossed by an anti-world attack segment, including attacks starting inside.
	 */
	public static void shatter(ServerLevel level, Vec3 from, Vec3 to) {
		for (RealityMarble marble : marbles(level.getServer())) {
			if (!intact(marble) || !marble.dimension().equals(level.dimension())) {
				continue;
			}
			Vec3 impact = MarbleSphere.impact(marble.center(), marble.radius(), from, to);
			if (impact != null) {
				marble.shatter(impact);
			}
		}
	}

	/**
	 * Closes the entity's marbles after teleporting outside them or into another dimension.
	 * Must not be called for ordinary walking.
	 */
	public static void displaced(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		for (RealityMarble marble : marbles(level.getServer())) {
			if (marble.phase() != RealityMarble.Phase.CLOSING && marble.caster().getUUID().equals(entity.getUUID())
				&& (!marble.dimension().equals(level.dimension()) || !marble.contains(entity.position()))) {
				marble.close();
			}
		}
	}

	// Queues the caster for the end of the tick, which is safe from an unload handler.
	private static void unavailable(LivingEntity entity, MinecraftServer server) {
		UUID caster = entity.getUUID();
		for (RealityMarble marble : marbles(server)) {
			if (marble.phase() != RealityMarble.Phase.CLOSING && marble.caster().getUUID().equals(caster)) {
				server.execute(() -> UNAVAILABLE.computeIfAbsent(server, key -> new HashSet<>()).add(caster));
				return;
			}
		}
	}

	private static void closeForced(MinecraftServer server) {
		Set<UUID> unavailable = UNAVAILABLE.remove(server);
		for (RealityMarble marble : marbles(server)) {
			if (marble.phase() != RealityMarble.Phase.CLOSING
				&& ((unavailable != null && unavailable.contains(marble.caster().getUUID())) || !held(server, marble))) {
				marble.close();
			}
		}
	}

	private static boolean held(MinecraftServer server, RealityMarble marble) {
		LivingEntity caster = marble.caster();
		return !caster.isRemoved() && caster.isAlive() && caster.level().dimension().equals(marble.dimension())
			&& AlayaServants.servantId(caster).equals(marble.servant())
			&& (!(caster instanceof ServerPlayer player) || server.getPlayerList().getPlayer(player.getUUID()) == player);
	}
}
