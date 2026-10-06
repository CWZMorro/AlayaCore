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

/** Intercepts attacks against entities and blocks sheltered by shields. */
public final class Shields {
	/** Bounds shield lookups in blocks; a shield's shape must fit within it. */
	public static final double MAX_RADIUS = 32.0;

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

	public static boolean shelters(ServerLevel level, Vec3 centre, Entity entity) {
		if (entity instanceof ShieldEntity) {
			return false;
		}
		return firstHit(level, centre, entity.getBoundingBox().getCenter()) != null;
	}

	/** Removes shielded blocks from {@code blocks} in place and returns the same list. */
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

	static @Nullable Vec3 origin(DamageSource source) {
		Entity direct = source.getDirectEntity();
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
