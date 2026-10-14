package io.github.cwzmorro.alayacore.util;

import net.minecraft.world.phys.Vec3;

/** Where something a servant holds out in front sits: a weapon's tip, a shield; and what lies to their right. */
public final class HeldInFront {
	private static final double DROP = 0.3;

	private HeldInFront() {
	}

	public static Vec3 right(Vec3 forward) {
		return new Vec3(-forward.z, 0, forward.x).normalize();
	}

	public static Vec3 at(Vec3 eye, Vec3 direction, double ahead) {
		return eye.add(direction.scale(ahead)).subtract(0, DROP, 0);
	}
}
