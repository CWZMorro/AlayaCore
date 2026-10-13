package io.github.cwzmorro.alayacore.combat;

import net.minecraft.world.phys.Vec3;

/**
 * Supplies a hit origin distinct from the attack entity's position for shield interception.
 */
@FunctionalInterface
public interface AttackOrigin {
	/** Returns the origin of a hit on the target's centre, in world coordinates. */
	Vec3 attackOrigin(Vec3 target);
}
