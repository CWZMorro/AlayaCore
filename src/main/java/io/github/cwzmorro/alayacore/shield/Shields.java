package io.github.cwzmorro.alayacore.shield;

import io.github.cwzmorro.alayacore.combat.AttackOrigin;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.network.ShieldConfigPayload;
import io.github.cwzmorro.alayacore.network.ShieldHitPayload;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Intercepts attacks against entities and blocks sheltered by shields. */
public final class Shields {
	/** Bounds shield lookups in blocks; larger shapes are rejected by {@link ShieldEntity#fitBox}. */
	public static final double MAX_RADIUS = 32.0;

	public record Hit(ShieldEntity shield, Vec3 entry) {
	}

	private Shields() {
	}

	public static void syncConfig(ServerPlayer player) {
		ServerPlayNetworking.send(player, new ShieldConfigPayload(AlayaConfig.shields()));
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(ShieldHitPayload.TYPE, ShieldHitPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ShieldConfigPayload.TYPE, ShieldConfigPayload.STREAM_CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncConfig(handler.getPlayer()));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) ->
			!(victim.level() instanceof ServerLevel level) || !intercepted(level, victim, source, amount));
	}

	/**
	 * Hurts {@code target}, any entity, unless a shield stands between the hit and it, in which case the shield takes
	 * it. Living targets get this check from any attacker; attacks that may also hit boats or item frames go through here.
	 *
	 * @return whether the target was hurt
	 */
	public static boolean hurt(ServerLevel level, Entity target, DamageSource source, float amount) {
		return !intercepted(level, target, source, amount) && target.hurtServer(level, source, amount);
	}

	private static boolean intercepted(ServerLevel level, Entity victim, DamageSource source, float amount) {
		// Explosions are stopped before they hurt or push anything (shelters).
		if (victim instanceof ShieldEntity || source.is(DamageTypeTags.IS_EXPLOSION)) {
			return false;
		}
		Vec3 centre = victim.getBoundingBox().getCenter();
		Vec3 from = origin(source, centre);
		Hit hit = from == null ? null : firstHit(level, from, centre);
		if (hit == null) {
			return false;
		}
		hit.shield().hurtAt(level, source, amount, hit.entry());
		return true;
	}

	public static boolean shelters(ServerLevel level, Vec3 centre, Entity entity) {
		if (entity instanceof ShieldEntity) {
			return false;
		}
		return firstHit(level, centre, entity.getBoundingBox().getCenter()) != null;
	}

	/** Removes shielded blocks from {@code blocks} in place and returns the same list. */
	public static List<BlockPos> unshielded(ServerLevel level, Vec3 centre, List<BlockPos> blocks) {
		return unshielded(level, target -> centre, blocks);
	}

	public static List<BlockPos> unshielded(ServerLevel level, AttackOrigin origin, List<BlockPos> blocks) {
		Optional<BoundingBox> reach = BoundingBox.encapsulatingPositions(blocks);
		if (reach.isEmpty()) {
			return blocks;
		}
		AABB area = AABB.of(reach.get());
		for (BlockPos pos : blocks) {
			Vec3 from = origin.attackOrigin(Vec3.atCenterOf(pos));
			area = area.minmax(new AABB(from, from));
		}
		List<ShieldEntity> shields = shieldsAround(level, area);
		if (!shields.isEmpty()) {
			blocks.removeIf(pos -> {
				Vec3 centre = Vec3.atCenterOf(pos);
				return firstHit(shields, origin.attackOrigin(centre), centre) != null;
			});
		}
		return blocks;
	}

	/**
	 * Hits the target or an intervening shield, adding its UUID to {@code struck}.
	 * Skips targets and shields already in that set.
	 */
	public static void strike(ServerLevel level, Vec3 from, Entity target, DamageSource source, float amount, Set<UUID> struck) {
		Hit shield = firstHit(level, from, target.getBoundingBox().getCenter());
		if (shield != null) {
			if (struck.add(shield.shield().getUUID())) {
				shield.shield().hurtAt(level, source, amount, shield.entry());
			}
		} else if (struck.add(target.getUUID())) {
			target.hurtServer(level, source, amount);
		}
	}

	static @Nullable Vec3 origin(DamageSource source, Vec3 target) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof AttackOrigin attack) {
			return attack.attackOrigin(target);
		}
		if (direct != null) {
			return lastPosition(direct).add(0, direct.getBbHeight() / 2, 0);
		}
		return source.getSourcePosition();
	}

	static Vec3 lastPosition(Entity entity) {
		return entity.tickCount > 0 ? entity.oldPosition() : entity.position();
	}

	/** Returns the first shield crossed by the segment, or null if none is crossed. */
	public static @Nullable Hit firstHit(ServerLevel level, Vec3 from, Vec3 to) {
		return firstHit(shieldsAround(level, new AABB(from, to)), from, to);
	}

	private static @Nullable Hit firstHit(List<ShieldEntity> shields, Vec3 from, Vec3 to) {
		Hit first = null;
		double nearest = Double.POSITIVE_INFINITY;
		for (ShieldEntity shield : shields) {
			Vec3 entry = shield.entry(from, to);
			double distance = entry == null ? Double.POSITIVE_INFINITY : entry.distanceToSqr(from);
			if (distance < nearest) {
				nearest = distance;
				first = new Hit(shield, entry);
			}
		}
		return first;
	}

	private static List<ShieldEntity> shieldsAround(ServerLevel level, AABB area) {
		return level.getEntitiesOfClass(ShieldEntity.class, area.inflate(MAX_RADIUS), Entity::isAlive);
	}
}
