package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Deflecting, a rule any ability opts into ({@link Ability#deflects()}): everything {@link AttackTags#deflectable} that is flying towards the deflecting point is pushed
 * away from it at its own speed, once per deflecting source.
 */
public final class Deflection {
	private Deflection() {
	}

	/**
	 * Deflects what is flying at {@code to} within {@code radius} of the path from {@code from} to {@code to}, for {@code ability}, if
	 * it deflects. {@code source} (the attack doing it) is never deflected itself, and what is in {@code already} is left alone;
	 * what is deflected is added to it.
	 */
	public static void deflect(ServerLevel level, Identifier ability, Entity source, Vec3 from, Vec3 to, double radius, Set<UUID> already) {
		if (!AlayaRegistries.ABILITY.getOptional(ability).map(Ability::deflects).orElse(false)) {
			return;
		}
		AABB envelope = new AABB(from, to).inflate(radius);
		for (Entity projectile : level.getEntities(source, envelope, AttackTags::deflectable)) {
			Vec3 velocity = projectile.getDeltaMovement();
			Vec3 away = projectile.position().subtract(to);
			if (velocity.dot(away) < 0 && already.add(projectile.getUUID())) {
				projectile.setDeltaMovement(away.normalize().scale(velocity.length()));
				projectile.hurtMarked = true;
			}
		}
	}
}
