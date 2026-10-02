package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Shields inside a running server (plan 9). The shield stands in the middle facing south (+Z): in front is the
 * south side, behind the north side.
 */
public class ShieldGameTests {
	private static final Vec3 SHIELD = new Vec3(4.5, 2.0, 4.5);
	private static final BlockPos BEHIND = new BlockPos(4, 1, 2);
	private static final BlockPos IN_FRONT = new BlockPos(4, 1, 6);
	/** Outside the dome, and just inside it. */
	private static final BlockPos OUTSIDE = new BlockPos(4, 1, 0);
	private static final BlockPos INSIDE = new BlockPos(5, 1, 5);
	private static final Rank RANK = Rank.parse("B");
	private static final float HIT = 10.0F;
	private static final float REPAIRED = 100.0F;
	private static final float EXPLOSION_POWER = 3.0F;
	/** How far in front of the shield the blast goes off, in blocks. */
	private static final double EXPLOSION_DISTANCE = 1.0;
	private static final double ARROW_SPEED = 1.5;
	private static final int ARROW_TICKS = 10;
	private static final int TICKS_HELD = 5;

	private static ShieldEntity shield(GameTestHelper helper, EntityType<? extends ShieldEntity> type, Rank rank) {
		ShieldEntity shield = helper.spawn(type, SHIELD);
		shield.setYRot(0.0F);
		shield.setXRot(0.0F);
		shield.deploy(rank);
		return shield;
	}

	/** A mob that stays where it is put; a husk, so daylight doesn't burn it. */
	private static Husk mob(GameTestHelper helper, BlockPos pos) {
		Husk mob = helper.spawn(EntityType.HUSK, pos);
		mob.setNoAi(true);
		mob.setNoGravity(true);
		return mob;
	}

	private static boolean hit(GameTestHelper helper, LivingEntity attacker, Entity target, float amount) {
		ServerLevel level = helper.getLevel();
		return target.hurtServer(level, level.damageSources().mobAttack(attacker), amount);
	}

	private static boolean hit(GameTestHelper helper, LivingEntity attacker, Entity target) {
		return hit(helper, attacker, target, HIT);
	}

	@GameTest
	public void aHitOnSomethingBehindLandsOnTheShield(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		Husk behind = mob(helper, BEHIND);
		check(helper, "hit blocked", false, hit(helper, mob(helper, IN_FRONT), behind));
		check(helper, "nothing behind is hurt", behind.getMaxHealth(), behind.getHealth());
		check(helper, "the shield took it", shield.maxHp() - HIT, shield.hp());
		helper.succeed();
	}

	@GameTest
	public void hittingTheShieldItselfDamagesItFromTheFrontOnly(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		check(helper, "hit from behind does nothing", false, hit(helper, mob(helper, BEHIND), shield));
		check(helper, "hit from the front", true, hit(helper, mob(helper, IN_FRONT), shield));
		check(helper, "HP", shield.maxHp() - HIT, shield.hp());
		helper.succeed();
	}

	@GameTest
	public void hitsFromBehindGoThrough(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		Husk inFront = mob(helper, IN_FRONT);
		check(helper, "hit lands", true, hit(helper, mob(helper, BEHIND), inFront));
		check(helper, "shield untouched", shield.maxHp(), shield.hp());
		helper.succeed();
	}

	@GameTest
	public void projectilesHitTheShieldInstead(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		Husk behind = mob(helper, BEHIND);
		Arrow arrow = new Arrow(helper.getLevel(), 0, 0, 0, new ItemStack(Items.ARROW), null);
		arrow.setPos(mob(helper, IN_FRONT).getBoundingBox().getCenter());
		arrow.setDeltaMovement(0, 0, -ARROW_SPEED);
		helper.getLevel().addFreshEntity(arrow);
		helper.runAfterDelay(ARROW_TICKS, () -> {
			check(helper, "nothing behind is hurt", behind.getMaxHealth(), behind.getHealth());
			check(helper, "the shield took the arrow", true, shield.hp() < shield.maxHp());
			helper.succeed();
		});
	}

	@GameTest
	public void explosionsInFrontSpareWhatIsBehind(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		Husk behind = mob(helper, BEHIND);
		Husk inFront = mob(helper, IN_FRONT);
		BlockPos blockBehind = BEHIND.west();
		BlockPos blockInFront = IN_FRONT.west();
		helper.setBlock(blockBehind, Blocks.DIRT);
		helper.setBlock(blockInFront, Blocks.DIRT);
		// No entity set it off, so the blast carries no position of its own.
		Vec3 centre = shield.position().add(0, 0, EXPLOSION_DISTANCE);
		helper.getLevel().explode(null, centre.x, centre.y, centre.z, EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
		helper.assertBlockPresent(Blocks.DIRT, blockBehind);
		helper.assertBlockNotPresent(Blocks.DIRT, blockInFront);
		check(helper, "nothing behind is hurt", behind.getMaxHealth(), behind.getHealth());
		check(helper, "nothing behind is pushed", Vec3.ZERO, behind.getDeltaMovement());
		check(helper, "in front is hurt", true, inFront.getHealth() < inFront.getMaxHealth());
		check(helper, "the shield took the blast", true, shield.hp() < shield.maxHp());
		helper.succeed();
	}

	@GameTest
	public void aFarHigherRankBreaksItAtOnce(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		Arrow attack = helper.spawn(EntityType.ARROW, IN_FRONT);
		attack.setAttached(Attacks.ATTACK, new AttackData(attack.getUUID(), Rank.parse("EX"), false));
		shield.hurtServer(helper.getLevel(), helper.getLevel().damageSources().arrow(attack, null), 1.0F);
		check(helper, "broken", true, shield.isRemoved());
		helper.succeed();
	}

	@GameTest
	public void itBreaksWhenItsHpRunsOut(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		hit(helper, mob(helper, IN_FRONT), mob(helper, BEHIND));
		check(helper, "still up", false, shield.isRemoved());
		hit(helper, mob(helper, IN_FRONT), shield, (float) shield.hp());
		check(helper, "broken", true, shield.isRemoved());
		helper.succeed();
	}

	@GameTest
	public void holdingPaysUpkeepAndRepairs(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		ServerPlayer holder = join(helper);
		double mana = AlayaMana.getOrCreate(holder).current();
		double upkeep = AlayaConfig.shields().holdUpkeepPerTick();
		shield.hold(holder);
		check(helper, "full: only the upkeep", mana - upkeep, AlayaMana.get(holder).current());

		hit(helper, mob(helper, IN_FRONT), shield, REPAIRED);
		shield.hold(holder);
		check(helper, "back to full", shield.maxHp(), shield.hp());
		check(helper, "upkeep and repair at the rank's rate", mana - 2 * upkeep - AlayaConfig.ranks().shieldRepairCost(RANK, REPAIRED),
			AlayaMana.get(holder).current());
		helper.succeed();
	}

	@GameTest
	public void aShieldStaysTheTapTimeOnceLetGo(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		check(helper, "HP from its rank", AlayaConfig.ranks().shieldHp(RANK), shield.maxHp());
		check(helper, "ticks left", AlayaConfig.shields().tapTicks(), shield.ticksLeft());
		helper.runAfterDelay(TICKS_HELD, () -> {
			shield.hold(join(helper));
			check(helper, "held: the tap time starts again", AlayaConfig.shields().tapTicks(), shield.ticksLeft());
			helper.succeed();
		});
	}

	@GameTest
	public void aDomeKeepsOutsideHitsOut(GameTestHelper helper) {
		ShieldEntity dome = shield(helper, TestShields.DOME, RANK);
		Husk inside = mob(helper, IN_FRONT);
		check(helper, "hit from outside blocked", false, hit(helper, mob(helper, OUTSIDE), inside));
		check(helper, "hit from inside lands", true, hit(helper, mob(helper, INSIDE), inside));
		check(helper, "the dome took the outside hit", dome.maxHp() - HIT, dome.hp());
		helper.succeed();
	}
}
