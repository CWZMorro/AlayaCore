package io.github.cwzmorro.alayacore.beam;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.AbilityCast;
import io.github.cwzmorro.alayacore.clash.BeamClashes;
import io.github.cwzmorro.alayacore.clash.ClashBeam;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.shield.Shields;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.SharedConstants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The rules every held beam shares: duration, drain, turn rate, clipping by clashes and shields, and the optional bow-like movement. */
public final class HeldBeam {
	// Absent outside the casts that opted in; the server supplies the configured input scale to clients.
	private static final AttachmentType<Double> MOVEMENT = AttachmentRegistry.create(AlayaCore.id("held_beam_movement"),
		builder -> builder.syncWith(ByteBufCodecs.DOUBLE, AttachmentSyncPredicate.all()));

	/** Begins bow-style input slowdown, not an attribute debuff; jump velocity is untouched. */
	public static void startMovement(LivingEntity entity) {
		entity.setAttached(MOVEMENT, AlayaConfig.heldBeams().heldBeamMovementMultiplier());
		entity.setSprinting(false);
	}

	public static void endMovement(LivingEntity entity) {
		entity.removeAttached(MOVEMENT);
	}

	public static boolean restrictsMovement(LivingEntity entity) {
		return entity.hasAttached(MOVEMENT);
	}

	public static float movementMultiplier(LivingEntity entity) {
		return entity.getAttachedOrElse(MOVEMENT, 1.0).floatValue();
	}

	private HeldBeam() {
	}

	/** Registers the movement attachment before Fabric negotiates syncable types at login. */
	public static void init() {
	}

	/**
	 * How far a beam reaches this tick: cut by a clash, then stopped by the first shield on the way.
	 *
	 * @param reach  distance from the beam's origin
	 * @param shield the shield that stopped it, or null
	 */
	public record Clip(double reach, Shields.@Nullable Hit shield) {
		public void hurtShield(ServerLevel level, DamageSource source, float damage) {
			if (this.shield != null) {
				this.shield.shield().hurtAt(level, source, damage, this.shield.entry());
			}
		}
	}

	public static Clip clip(ServerLevel level, ClashBeam beam) {
		return clip(level, beam, beam.beamOrigin(), beam.beamDirection(), beam.beamLength());
	}

	public static Clip clip(ServerLevel level, ClashBeam beam, Vec3 origin, Vec3 end) {
		return clip(level, beam, origin, end.subtract(origin).normalize(), origin.distanceTo(end));
	}

	private static Clip clip(ServerLevel level, ClashBeam beam, Vec3 origin, Vec3 direction, double length) {
		double cut = BeamClashes.reach(beam.beamEntity(), length);
		Shields.Hit shield = Shields.firstHit(level, origin, origin.add(direction.scale(cut)));
		return new Clip(shield == null ? cut : shield.entry().distanceTo(origin), shield);
	}

	public static boolean payTick(AbilityCast cast, Rank rank, int firing) {
		int limit = AlayaConfig.ranks().beamMaxHoldSeconds() * SharedConstants.TICKS_PER_SECOND;
		if (firing >= limit) {
			cast.end();
			return false;
		}
		return cast.pay(AlayaConfig.ranks().beamDrainPerSecond(rank) / SharedConstants.TICKS_PER_SECOND);
	}

	public static Vec3 turned(Vec3 from, Vec3 to) {
		double most = Math.toRadians(AlayaConfig.ranks().beamTurnDegreesPerSecond()) / SharedConstants.TICKS_PER_SECOND;
		double angle = Math.acos(Mth.clamp(from.dot(to), -1.0, 1.0));
		if (angle <= most) {
			return to;
		}
		Vec3 axis = from.cross(to);
		if (axis.lengthSqr() == 0) {
			return from;
		}
		axis = axis.normalize();
		// Rodrigues' rotation preserves unit length while limiting the angular step.
		return from.scale(Math.cos(most)).add(axis.cross(from).scale(Math.sin(most)))
			.add(axis.scale(axis.dot(from) * (1 - Math.cos(most))));
	}
}
