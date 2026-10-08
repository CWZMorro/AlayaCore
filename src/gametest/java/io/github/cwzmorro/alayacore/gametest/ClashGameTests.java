package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.clash.BeamClashes;
import io.github.cwzmorro.alayacore.clash.ClashBeam;
import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.rank.Rank;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.phys.Vec3;

public class ClashGameTests {
	private static final double LENGTH = 10.0;
	private static final double RADIUS = 0.5;
	private static final double MANA = 100.0;
	private static final double DRAIN = 20.0;
	private static final double ORDINARY_PAID = 1.0;
	private static final double EPSILON = 0.00001;
	private static final float HIT = 10.0F;
	private static final Vec3 A = new Vec3(1, 2, 1);
	private static final Vec3 B = A.add(LENGTH, 0, 0);
	private static final Vec3 EAST = new Vec3(1, 0, 0);

	@GameTest
	public void meetingCutsBothBeforeEffects(GameTestHelper helper) {
		TestBeam a = beam(helper, A, EAST, "A", true);
		TestBeam b = beam(helper, B, EAST.scale(-1), "A", true);
		new BeamClashes().resolve(helper.getLevel(), List.of(a, b));
		check(helper, "both cut halfway before hitting", true, close(a.hitReach, LENGTH / 2) && close(b.hitReach, LENGTH / 2));
		helper.succeed();
	}

	@GameTest
	public void farRanksWinAtOnceOnTheirOwnPath(GameTestHelper helper) {
		TestBeam a = beam(helper, A, EAST, "EX", true);
		TestBeam b = beam(helper, B, EAST.scale(-1), "D", true);
		new BeamClashes().resolve(helper.getLevel(), List.of(a, b));
		check(helper, "lower ended without hitting", true, !b.held && b.effects == 0);
		check(helper, "winner uncut", true, close(a.hitReach, LENGTH));
		check(helper, "winner's continuation damage", true, close(BeamClashes.damage(a.entity, HIT), AlayaConfig.clash().winnerContinuationDamage(HIT)));
		helper.succeed();
	}

	@GameTest
	public void sameRankFirstToStopLoses(GameTestHelper helper) {
		TestBeam a = beam(helper, A, EAST, "A", true);
		TestBeam b = beam(helper, B, EAST.scale(-1), "A", true);
		BeamClashes tracker = new BeamClashes();
		tracker.resolve(helper.getLevel(), List.of(a, b));
		b.held = false;
		tracker.resolve(helper.getLevel(), List.of(a));
		check(helper, "release unclips survivor", true, close(a.hitReach, LENGTH));
		check(helper, "release gives the winner its continuation damage", true,
			close(BeamClashes.damage(a.entity, HIT), AlayaConfig.clash().winnerContinuationDamage(HIT)));
		helper.succeed();
	}

	@GameTest
	public void lowerRankPaysOnlyTheExtraOfItsOwnDrain(GameTestHelper helper) {
		TestBeam a = beam(helper, A, EAST, "EX", true);
		TestBeam b = beam(helper, B, EAST.scale(-1), "A++", true);
		// The held casts pay their ordinary drain before world resolution.
		AlayaMana.spend(a.entity, ORDINARY_PAID);
		AlayaMana.spend(b.entity, ORDINARY_PAID);
		new BeamClashes().resolve(helper.getLevel(), List.of(a, b));
		double extra = DRAIN * (AlayaConfig.clash().closeLowerDrainMultiplier() - 1.0) / SharedConstants.TICKS_PER_SECOND;
		check(helper, "higher only pays ordinary drain", true, close(AlayaMana.available(a.entity), MANA - ORDINARY_PAID));
		check(helper, "lower pays the extra on its own drain", true, close(AlayaMana.available(b.entity), MANA - ORDINARY_PAID - extra));
		check(helper, "contact nearer lower", true, a.hitReach > LENGTH / 2 && b.hitReach < LENGTH / 2);
		helper.succeed();
	}

	@GameTest
	public void exhaustedSideEndsAndBurstDoesNotClash(GameTestHelper helper) {
		TestBeam a = beam(helper, A, EAST, "A", true);
		TestBeam b = beam(helper, B, EAST.scale(-1), "A", true);
		BeamClashes tracker = new BeamClashes();
		tracker.resolve(helper.getLevel(), List.of(a, b));
		AlayaMana.update(b.entity, mana -> mana.withCurrent(0));
		tracker.resolve(helper.getLevel(), List.of(a, b));
		check(helper, "exhausted side ended", true, !b.held);
		check(helper, "winner continues", true, close(a.hitReach, LENGTH));
		TestBeam burst = beam(helper, B, EAST.scale(-1), "EX", false);
		tracker.resolve(helper.getLevel(), List.of(a, burst));
		check(helper, "burst cannot clash", true, burst.held && close(burst.hitReach, LENGTH));
		helper.succeed();
	}

	@GameTest
	public void finiteSegmentsMustActuallyMeet(GameTestHelper helper) {
		TestBeam a = beam(helper, A, EAST, "A", true);
		TestBeam b = beam(helper, B.add(0, 0, RADIUS * 3), EAST.scale(-1), "A", true);
		new BeamClashes().resolve(helper.getLevel(), List.of(a, b));
		check(helper, "nearby parallel paths do not meet", true, close(a.hitReach, LENGTH) && close(b.hitReach, LENGTH));
		helper.succeed();
	}

	private static boolean close(double a, double b) {
		return Math.abs(a - b) < EPSILON;
	}

	private static TestBeam beam(GameTestHelper helper, Vec3 origin, Vec3 direction, String rank, boolean canClash) {
		Husk entity = helper.spawn(EntityType.HUSK, origin);
		entity.setNoAi(true);
		entity.setNoGravity(true);
		AlayaMana.getOrCreate(entity);
		AlayaMana.update(entity, mana -> mana.withMax(MANA).withCurrent(MANA));
		entity.setAttached(Attacks.ATTACK, new AttackData(entity.getUUID(), Rank.parse(rank), canClash, true));
		return new TestBeam(entity, entity.position(), direction);
	}

	private static final class TestBeam implements ClashBeam {
		private final Husk entity;
		private final Vec3 origin;
		private final Vec3 direction;
		private boolean held = true;
		private double hitReach;
		private int effects;

		private TestBeam(Husk entity, Vec3 origin, Vec3 direction) {
			this.entity = entity;
			this.origin = origin;
			this.direction = direction;
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
			return LENGTH;
		}

		@Override
		public double beamRadius() {
			return RADIUS;
		}

		@Override
		public LivingEntity beamOwner() {
			return this.entity;
		}

		@Override
		public double beamDrainPerSecond() {
			return DRAIN;
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
			this.hitReach = BeamClashes.reach(this.entity, LENGTH);
			this.effects++;
		}
	}
}
