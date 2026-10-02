package io.github.cwzmorro.alayacore.shield;

import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What shields stop (plan 9): nothing behind a shield is damaged until it breaks, blocks included. */
public final class Shields {
	/** The largest shield radius the lookups cover, in blocks: a shield's shape must fit within it. */
	public static final double MAX_RADIUS = 32.0;

	/** A shield an attack's path runs into, and where. */
	public record Hit(ShieldEntity shield, Vec3 entry) {
	}

	private Shields() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			Vec3 from = origin(source);
			// Explosions are stopped before they hurt or push anything (shelters).
			if (from == null || source.is(DamageTypeTags.IS_EXPLOSION) || !(victim.level() instanceof ServerLevel level)) {
				return true;
			}
			Hit hit = firstHit(level, from, victim.getBoundingBox().getCenter());
			if (hit == null) {
				return true;
			}
			hit.shield().hurtServer(level, source, amount);
			return false;
		});
	}

	/** True if a shield stands between an explosion at {@code centre} and {@code entity} (not a shield itself). */
	public static boolean shelters(ServerLevel level, Vec3 centre, Entity entity) {
		if (entity instanceof ShieldEntity) {
			return false;
		}
		return firstHit(level, centre, entity.getBoundingBox().getCenter()) != null;
	}

	/** The blocks an explosion at {@code centre} may break: none that a shield stands in front of. Changes {@code blocks}. */
	public static List<BlockPos> unshielded(ServerLevel level, Vec3 centre, List<BlockPos> blocks) {
		if (blocks.isEmpty()) {
			return blocks;
		}
		List<ShieldEntity> shields = shieldsAround(level, new AABB(centre, centre));
		if (!shields.isEmpty()) {
			blocks.removeIf(pos -> firstHit(shields, centre, Vec3.atCenterOf(pos)) != null);
		}
		return blocks;
	}

	/** Where a hit comes from: the entity that dealt it, at the start of its last move, else the position it names; null if neither. */
	static @Nullable Vec3 origin(DamageSource source) {
		Entity direct = source.getDirectEntity();
		if (direct != null) {
			return lastPosition(direct).add(0, direct.getBbHeight() / 2, 0);
		}
		return source.getSourcePosition();
	}

	/** Where {@code entity} was at the start of its last move; where it is if it hasn't moved yet (its old position is unset until then). */
	static Vec3 lastPosition(Entity entity) {
		return entity.tickCount > 0 ? entity.oldPosition() : entity.position();
	}

	/** The shield the path from {@code from} to {@code to} runs into first (a beam stops there), if any. */
	public static @Nullable Hit firstHit(ServerLevel level, Vec3 from, Vec3 to) {
		return firstHit(shieldsAround(level, new AABB(from, to)), from, to);
	}

	/** Of {@code shields}, the one the path from {@code from} to {@code to} enters first, if any. */
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

	/** The live shields that could reach into {@code area}. */
	private static List<ShieldEntity> shieldsAround(ServerLevel level, AABB area) {
		return level.getEntitiesOfClass(ShieldEntity.class, area.inflate(MAX_RADIUS), Entity::isAlive);
	}
}
