package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;

import io.github.cwzmorro.alayacore.beam.BeamVolume;
import io.github.cwzmorro.alayacore.beam.HeldBeam;
import io.github.cwzmorro.alayacore.bow.ArcherBows;
import io.github.cwzmorro.alayacore.clash.ClashBeam;
import io.github.cwzmorro.alayacore.combat.DashMovement;
import io.github.cwzmorro.alayacore.combat.ForcedAdvance;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.item.WeaponRules;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.thrown.ThrownWeaponEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class WeaponGameTests {
	// The disc shield stands here facing south (+Z); the beam comes from the south, north along -Z, at the shield's middle.
	private static final Vec3 SHIELD = new Vec3(4.5, 2.0, 4.5);
	private static final Vec3 BEAM_ORIGIN = new Vec3(4.5, 2.5, 7.5);
	private static final Vec3 NORTH = new Vec3(0, 0, -1);
	private static final double BEAM_LENGTH = 7.0;
	private static final double BEAM_RADIUS = 1.0;
	private static final double CARVE_RADIUS = 0.5;
	private static final BlockPos IN_FRONT = new BlockPos(4, 1, 6);
	private static final BlockPos BEHIND = new BlockPos(4, 1, 2);
	private static final BlockPos CARVED = new BlockPos(4, 2, 5);
	private static final BlockPos SPARED = new BlockPos(4, 2, 3);
	private static final Rank RANK = Rank.parse("B");
	private static final float DAMAGE = 5.0F;
	private static final BlockPos THROWER = new BlockPos(3, 1, 0);
	private static final BlockPos TARGET = new BlockPos(3, 1, 3);
	private static final Vec3 THROWN_FROM = new Vec3(3.5, 2.5, 1.5);
	private static final Vec3 THROWN_VELOCITY = new Vec3(0, 0, 3);
	private static final BlockPos DASHER = new BlockPos(1, 1, 1);
	private static final int WALL_Z = 4;
	private static final Vec3 DASH = new Vec3(0, 0, 5);
	private static final double DASH_STEP = 0.5;
	// Where the dash stops short of the wall: the last whole step before the body would touch it.
	private static final double STOPPED_AT = 2.0;
	private static final double CLOSE = 0.01;
	private static final double ADVANCE_SPEED = 0.25;
	private static final int ADVANCE_TICKS = 4;
	private static final float SOUTH = 0.0F;
	private static final float WEST = 90.0F;

	private static Husk mob(GameTestHelper helper, BlockPos pos) {
		Husk mob = helper.spawn(EntityType.HUSK, pos);
		mob.setNoAi(true);
		mob.setNoGravity(true);
		return mob;
	}

	@GameTest
	public void aHeldBeamTurnsAndSlowsItsCasterAsConfigured(GameTestHelper helper) {
		Vec3 from = new Vec3(0, 0, 1);
		Vec3 turned = HeldBeam.turned(from, new Vec3(1, 0, 0));
		double most = Math.toRadians(AlayaConfig.ranks().beamTurnDegreesPerSecond()) / SharedConstants.TICKS_PER_SECOND;
		check(helper, "one tick's turn at the configured rate", most, Math.acos(turned.dot(from)));
		Vec3 slight = new Vec3(Math.sin(most / 2), 0, Math.cos(most / 2));
		check(helper, "a smaller turn is made whole", slight, HeldBeam.turned(from, slight));

		Husk caster = mob(helper, IN_FRONT);
		HeldBeam.startMovement(caster);
		caster.setSprinting(true);
		check(helper, "slowed by the configured multiplier, no sprinting", true, HeldBeam.restrictsMovement(caster) && !caster.isSprinting()
			&& Math.abs(HeldBeam.movementMultiplier(caster) - AlayaConfig.heldBeams().heldBeamMovementMultiplier()) < CLOSE);
		HeldBeam.endMovement(caster);
		check(helper, "free again", true, !HeldBeam.restrictsMovement(caster) && HeldBeam.movementMultiplier(caster) == 1.0F);
		helper.succeed();
	}

	@GameTest
	public void aHeldBeamStopsAtAShieldAndHitsAndCarvesOnlyInFrontOfIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		TestShields.TestShield shield = helper.spawn(TestShields.DISC, SHIELD);
		shield.setYRot(0.0F);
		shield.setXRot(0.0F);
		shield.deploy(RANK);
		Husk caster = mob(helper, new BlockPos(0, 1, 7));
		Husk inFront = mob(helper, IN_FRONT);
		Husk behind = mob(helper, BEHIND);
		helper.setBlock(CARVED, Blocks.STONE);
		helper.setBlock(SPARED, Blocks.STONE);
		Vec3 origin = helper.absoluteVec(BEAM_ORIGIN);
		TestBeam beam = new TestBeam(caster, origin);

		HeldBeam.Clip clip = HeldBeam.clip(level, beam);
		check(helper, "the beam ends where it enters the shield", true, clip.shield() != null);
		check(helper, "its reach", BEAM_ORIGIN.z - SHIELD.z, clip.reach());
		DamageSource source = level.damageSources().mobAttack(caster);
		clip.hurtShield(level, source, DAMAGE);
		check(helper, "the shield takes the beam", 1, shield.hits);
		BeamVolume.hit(level, caster, caster, source, DAMAGE, origin, NORTH, clip.reach(), along -> BEAM_RADIUS);
		BeamVolume.carve(level, origin, NORTH, clip.reach(), along -> CARVE_RADIUS);
		check(helper, "what is in front is hit", true, inFront.getHealth() < inFront.getMaxHealth());
		check(helper, "what is behind is not", behind.getMaxHealth(), behind.getHealth());
		check(helper, "the block in front is carved", true, helper.getBlockState(CARVED).isAir());
		check(helper, "the block behind stays", true, helper.getBlockState(SPARED).is(Blocks.STONE));
		helper.succeed();
	}

	@GameTest
	public void aThrownWeaponStrikesWithItsOwnAttackAndIsUsedUp(GameTestHelper helper) {
		Husk thrower = mob(helper, THROWER);
		Husk target = mob(helper, TARGET);
		ThrownWeaponEntity weapon = ThrownWeaponEntity.launch(helper.getLevel(), TestWeapons.THROWN, thrower, new ItemStack(Items.IRON_SWORD),
			helper.absoluteVec(THROWN_FROM), THROWN_VELOCITY, DamageTypes.TRIDENT);
		weapon.tick();
		DamageSource source = target.getLastDamageSource();
		check(helper, "the target is struck", true, target.getHealth() < target.getMaxHealth());
		check(helper, "as its launcher's damage type, by its thrower and itself", true,
			source != null && source.is(DamageTypes.TRIDENT) && source.getEntity() == thrower && source.getDirectEntity() == weapon);
		check(helper, "used up where it strikes", true, weapon.isRemoved());
		check(helper, "its thrower is not struck", thrower.getMaxHealth(), thrower.getHealth());
		helper.succeed();
	}

	@GameTest
	public void aDashStopsAtAWallOrCarvesThroughIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		check(helper, "weapons break blocks by default", true, level.getGameRules().get(AlayaGameRules.WEAPON_BLOCK_DAMAGE));
		BlockPos low = new BlockPos(DASHER.getX(), DASHER.getY(), WALL_Z);
		BlockPos high = low.above();
		helper.setBlock(low, Blocks.STONE);
		helper.setBlock(high, Blocks.STONE);
		Husk dasher = mob(helper, DASHER);
		Vec3 start = dasher.position();

		DashMovement.advance(level, dasher, DASH, DASH_STEP, false);
		check(helper, "stopped short of the wall", STOPPED_AT, dasher.position().distanceTo(start));
		check(helper, "the wall stands", true, helper.getBlockState(low).is(Blocks.STONE) && helper.getBlockState(high).is(Blocks.STONE));

		dasher.setPos(start);
		DashMovement.advance(level, dasher, DASH, DASH_STEP, true);
		check(helper, "the whole dash", DASH.length(), dasher.position().distanceTo(start));
		check(helper, "the wall is carved", true, helper.getBlockState(low).isAir() && helper.getBlockState(high).isAir());
		helper.succeed();
	}

	@GameTest
	public void aBodyCanCarveItsWayWithoutBeingMoved(GameTestHelper helper) {
		BlockPos wall = new BlockPos(DASHER.getX(), DASHER.getY(), WALL_Z);
		helper.setBlock(wall, Blocks.STONE);
		helper.setBlock(wall.above(), Blocks.STONE);
		Husk body = mob(helper, DASHER);
		Vec3 start = body.position();
		DashMovement.carve(helper.getLevel(), body, DASH, DASH_STEP);
		check(helper, "the wall is carved", true, helper.getBlockState(wall).isAir() && helper.getBlockState(wall.above()).isAir());
		check(helper, "the body stays where it is", start, body.position());
		helper.succeed();
	}

	@GameTest
	public void anAdvancedBodyMovesLevelAlongItsFacingUntilLetGo(GameTestHelper helper) {
		Husk body = helper.spawn(EntityType.HUSK, DASHER);
		body.setNoAi(true);
		body.setYRot(SOUTH);
		Vec3 start = body.position();
		ForcedAdvance.set(body, ADVANCE_SPEED);
		check(helper, "advancing", true, ForcedAdvance.active(body));
		for (int tick = 0; tick < ADVANCE_TICKS; tick++) {
			body.travel(Vec3.ZERO);
		}
		check(helper, "south, a step a tick", true, Math.abs(ADVANCE_SPEED * ADVANCE_TICKS - (body.getZ() - start.z)) < CLOSE);
		check(helper, "no sideways drift", true, Math.abs(start.x - body.getX()) < CLOSE);
		body.setYRot(WEST);
		Vec3 turned = body.position();
		for (int tick = 0; tick < ADVANCE_TICKS; tick++) {
			body.travel(Vec3.ZERO);
		}
		check(helper, "turning turns the advance", true, Math.abs(ADVANCE_SPEED * ADVANCE_TICKS - (turned.x - body.getX())) < CLOSE);
		check(helper, "the advance never lifts it: gravity is its own", true, body.getY() <= start.y + CLOSE);
		ForcedAdvance.stop(body);
		check(helper, "let go", false, ForcedAdvance.active(body));
		helper.succeed();
	}

	@GameTest
	public void anyArchersBowDrawsAndShootsFasterByItsBarAndAHumansDoesNot(GameTestHelper helper) {
		ServerPlayer archer = TestPlayers.joinAs(helper, TestServants.BOWMAN);
		double bar = ArcherBows.bar(archer);
		check(helper, "an Archer", true, ArcherBows.archer(archer));
		check(helper, "draw ticks by bar", BowItem.MAX_DRAW_DURATION - AlayaConfig.archerBows().archerDrawReduction() * bar,
			ArcherBows.drawTicks(bar));
		int full = (int) Math.ceil(ArcherBows.drawTicks(bar));
		check(helper, "fully drawn at its draw ticks, not before", true, ArcherBows.power(archer, full) == 1 && ArcherBows.power(archer, full - 1) < 1);
		check(helper, "arrow speed by bar", 1 + AlayaConfig.archerBows().archerSpeedBonus() * bar, ArcherBows.speedMultiplier(bar));
		check(helper, "its class attack", true, ArcherBows.attack(archer) > 0);
		ServerPlayer human = TestPlayers.joinAsHuman(helper);
		check(helper, "a human's bow is vanilla's", true, !ArcherBows.archer(human) && ArcherBows.bar(human) == 0
			&& ArcherBows.drawTicks(ArcherBows.bar(human)) == BowItem.MAX_DRAW_DURATION && ArcherBows.attack(human) == 0);
		helper.succeed();
	}

	@GameTest
	public void taggedWeaponsAreKeptThroughDeathAndNeverDestroyedWhenDropped(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		player.getInventory().add(new ItemStack(Items.GOLDEN_SWORD));
		player.getInventory().add(new ItemStack(Items.IRON_SWORD));
		player.getInventory().dropAll();
		check(helper, "only the tagged weapon is kept", true, player.getAttachedOrElse(WeaponRules.KEPT, List.of()).size() == 1
			&& player.getAttached(WeaponRules.KEPT).getFirst().is(Items.GOLDEN_SWORD));
		WeaponRules.giveBack(player);
		check(helper, "given back", true, player.getInventory().contains(stack -> stack.is(Items.GOLDEN_SWORD))
			&& player.getAttached(WeaponRules.KEPT) == null);
		ServerLevel level = helper.getLevel();
		Vec3 at = helper.absoluteVec(THROWN_FROM);
		ItemEntity tagged = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.GOLDEN_SWORD));
		ItemEntity other = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.IRON_SWORD));
		level.addFreshEntity(tagged);
		level.addFreshEntity(other);
		check(helper, "the tagged weapon dropped is indestructible", true, tagged.isInvulnerable() && !other.isInvulnerable());
		helper.succeed();
	}

	private static final class TestBeam implements ClashBeam {
		private final Husk caster;
		private final Vec3 origin;

		private TestBeam(Husk caster, Vec3 origin) {
			this.caster = caster;
			this.origin = origin;
		}

		@Override
		public Entity beamEntity() {
			return this.caster;
		}

		@Override
		public Vec3 beamOrigin() {
			return this.origin;
		}

		@Override
		public Vec3 beamDirection() {
			return NORTH;
		}

		@Override
		public double beamLength() {
			return BEAM_LENGTH;
		}

		@Override
		public double beamRadius() {
			return BEAM_RADIUS;
		}

		@Override
		public LivingEntity beamOwner() {
			return this.caster;
		}

		@Override
		public double beamDrainPerSecond() {
			return 0;
		}

		@Override
		public boolean beamLive() {
			return true;
		}

		@Override
		public void loseClash() {
		}

		@Override
		public void beamEffects(ServerLevel level) {
		}
	}
}
