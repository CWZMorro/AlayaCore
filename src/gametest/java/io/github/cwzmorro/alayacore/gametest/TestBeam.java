package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.clash.BeamClashes;
import io.github.cwzmorro.alayacore.clash.ClashBeam;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class TestBeam implements ClashBeam {
	final LivingEntity entity;
	private final Vec3 origin;
	private final Vec3 direction;
	private final double length;
	private final double radius;
	private final double drainPerSecond;
	boolean held = true;
	double hitReach;
	int effects;

	TestBeam(LivingEntity entity, Vec3 origin, Vec3 direction, double length, double radius, double drainPerSecond) {
		this.entity = entity;
		this.origin = origin;
		this.direction = direction;
		this.length = length;
		this.radius = radius;
		this.drainPerSecond = drainPerSecond;
	}

	@Override
	public Entity beamEntity() {
		return this.entity;
	}

	@Override
	public Vec3 beamOrigin() {
		return this.origin;
	}

	@Override
	public Vec3 beamDirection() {
		return this.direction;
	}

	@Override
	public double beamLength() {
		return this.length;
	}

	@Override
	public double beamRadius() {
		return this.radius;
	}

	@Override
	public LivingEntity beamOwner() {
		return this.entity;
	}

	@Override
	public double beamDrainPerSecond() {
		return this.drainPerSecond;
	}

	@Override
	public boolean beamLive() {
		return this.held;
	}

	@Override
	public void loseClash() {
		this.held = false;
	}

	@Override
	public void beamEffects(ServerLevel level) {
		this.hitReach = BeamClashes.reach(this.entity, this.length);
		this.effects++;
	}
}
