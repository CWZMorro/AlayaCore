package io.github.cwzmorro.alayacore.clash;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A world beam that takes part in clashes. Unranked attacks and other mods' ordinary attacks are not considered. */
public interface ClashBeam {
	Entity beamEntity();

	Vec3 beamOrigin();

	/** Returns the beam's unit direction along its own path. */
	Vec3 beamDirection();

	double beamLength();

	double beamRadius();

	/** Returns the caster, or null if the caster has left the world. */
	@Nullable LivingEntity beamOwner();

	/** Returns the ordinary per-second drain already paid by the cast, excluding clash surcharges. */
	double beamDrainPerSecond();

	boolean beamLive();

	/** Ends this beam and its cast, with the normal ability cleanup. */
	void loseClash();

	void beamEffects(ServerLevel level);
}
