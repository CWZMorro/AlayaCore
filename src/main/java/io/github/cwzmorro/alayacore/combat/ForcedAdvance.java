package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Drives an entity forward using a server-synced speed, preserving vanilla gravity and jumping.
 * Movement runs on the owning side: the local client for players and the server for mobs.
 */
public final class ForcedAdvance {
	// Blocks per tick; absent when the body is not being advanced.
	private static final AttachmentType<Double> SPEED = AttachmentRegistry.create(AlayaCore.id("forced_advance"),
		builder -> builder.syncWith(ByteBufCodecs.DOUBLE, AttachmentSyncPredicate.targetOnly()));

	private ForcedAdvance() {
	}

	/** Registers the attachment before Fabric negotiates syncable types at login. */
	public static void init() {
	}

	public static void set(LivingEntity entity, double blocksPerTick) {
		if (!Double.valueOf(blocksPerTick).equals(entity.getAttached(SPEED))) {
			entity.setAttached(SPEED, blocksPerTick);
		}
	}

	public static void stop(LivingEntity entity) {
		entity.removeAttached(SPEED);
	}

	public static boolean active(LivingEntity entity) {
		return entity.hasAttached(SPEED);
	}

	/** Returns the horizontal heading from the entity's current view. */
	public static Vec3 heading(LivingEntity entity) {
		double yaw = Math.toRadians(entity.getYRot());
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	public static void drive(LivingEntity entity) {
		Double speed = entity.getAttached(SPEED);
		if (speed != null) {
			Vec3 motion = entity.getDeltaMovement();
			Vec3 along = heading(entity).scale(speed);
			entity.setDeltaMovement(along.x, motion.y, along.z);
		}
	}
}
