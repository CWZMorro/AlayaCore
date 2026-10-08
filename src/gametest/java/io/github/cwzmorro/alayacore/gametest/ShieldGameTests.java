package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.network.ShieldHitPayload;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import io.github.cwzmorro.alayacore.shield.Shields;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ShieldGameTests {
	private static final Vec3 SHIELD = new Vec3(4.5, 2.0, 4.5);
	private static final BlockPos BEHIND = new BlockPos(4, 1, 2);
	private static final BlockPos IN_FRONT = new BlockPos(4, 1, 6);
	private static final BlockPos OUTSIDE = new BlockPos(4, 1, 0);
	private static final BlockPos INSIDE = new BlockPos(5, 1, 5);
	private static final Rank RANK = Rank.parse("B");
	private static final float HIT = 10.0F;
	private static final float REPAIRED = 100.0F;
	private static final float EXPLOSION_POWER = 3.0F;
	private static final double EXPLOSION_DISTANCE = 1.0;
	private static final double ARROW_SPEED = 1.5;
	private static final int ARROW_TICKS = 10;
	private static final int TICKS_HELD = 5;
	private static final Vec3 OFF_CENTRE = new Vec3(1, 0, 0);
	private static final float LOOKING_STRAIGHT_UP = -90.0F;
	private static final double CLOSE = 0.001;
	// Mock clients send no chunk acknowledgements: the chunks this near them are marked tracked, and tracking starts within the ticks after.
	private static final int TRACKING_RADIUS = 2;
	private static final int TRACKING_TICKS = 10;

	private static <T extends ShieldEntity> T shield(GameTestHelper helper, EntityType<T> type, Rank rank) {
		T shield = helper.spawn(type, SHIELD);
		shield.setYRot(0.0F);
		shield.setXRot(0.0F);
		shield.deploy(rank);
		return shield;
	}

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

	private static void shootNorth(GameTestHelper helper, Vec3 from) {
		Arrow arrow = new Arrow(helper.getLevel(), 0, 0, 0, new ItemStack(Items.ARROW), null);
		arrow.setPos(from);
		arrow.setNoGravity(true);
		arrow.setDeltaMovement(0, 0, -ARROW_SPEED);
		helper.getLevel().addFreshEntity(arrow);
	}

	private static ServerPlayer watcher(GameTestHelper helper, BlockPos pos) {
		ServerPlayer player = join(helper);
		player.snapTo(helper.absoluteVec(Vec3.atBottomCenterOf(pos)), 0.0F, 0.0F);
		player.setChunkTrackingView(ChunkTrackingView.of(player.chunkPosition(), TRACKING_RADIUS));
		return player;
	}

	private static List<ShieldHitPayload> hits(ServerPlayer player) {
		List<ShieldHitPayload> hits = new ArrayList<>();
		ServerGamePacketListenerImpl original = player.connection;
		new ServerGamePacketListenerImpl(player.level().getServer(), new Connection(PacketFlow.SERVERBOUND), player,
			CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
			@Override
			public void send(Packet<?> packet) {
				if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof ShieldHitPayload hit) {
					hits.add(hit);
				} else {
					original.send(packet);
				}
			}
		};
		return hits;
	}

	private static List<ShieldHitPayload> hitsOn(List<ShieldHitPayload> hits, ShieldEntity shield) {
		return hits.stream().filter(hit -> hit.shieldId() == shield.getId()).toList();
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
		shootNorth(helper, mob(helper, IN_FRONT).getBoundingBox().getCenter());
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
		attack.setAttached(Attacks.ATTACK, new AttackData(attack.getUUID(), Rank.parse("EX"), false, false));
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
		holder.setGameMode(GameType.SURVIVAL);
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
	public void redirectedHitsReportTheirEntryAndHpTaken(GameTestHelper helper) {
		TestShields.TestShield shield = shield(helper, TestShields.DISC, RANK);
		Husk attacker = mob(helper, IN_FRONT.east());
		Husk behind = mob(helper, BEHIND.east());
		Shields.Hit expected = Shields.firstHit(helper.getLevel(), attacker.getBoundingBox().getCenter(), behind.getBoundingBox().getCenter());
		hit(helper, attacker, behind);
		check(helper, "one hit", 1, shield.hits);
		check(helper, "crossing", expected.entry(), shield.hitPoint);
		check(helper, "HP taken", (double) HIT, shield.hpTaken);
		helper.succeed();
	}

	@GameTest
	public void directHitsUseTheLookRayThenFallBackToTheCentre(GameTestHelper helper) {
		TestShields.TestShield shield = shield(helper, TestShields.DISC, RANK);
		Husk attacker = mob(helper, IN_FRONT);
		Vec3 aimed = shield.position().add(OFF_CENTRE);
		attacker.lookAt(EntityAnchorArgument.Anchor.EYES, aimed);
		hit(helper, attacker, shield);
		check(helper, "look ray on disc", true, shield.hitPoint.distanceTo(aimed) < CLOSE);
		attacker.setXRot(LOOKING_STRAIGHT_UP);
		hit(helper, attacker, shield);
		check(helper, "fallback to centre", shield.position(), shield.hitPoint);
		hit(helper, mob(helper, BEHIND), shield);
		hit(helper, attacker, shield, 0.0F);
		check(helper, "no hook for refused or zero damage hits", 2, shield.hits);
		helper.succeed();
	}

	@GameTest
	public void projectileHitsReportTheExactCrossing(GameTestHelper helper) {
		TestShields.TestShield shield = shield(helper, TestShields.DISC, RANK);
		Vec3 point = shield.position().add(OFF_CENTRE);
		shootNorth(helper, point.add(0, 0, 1));
		helper.runAfterDelay(ARROW_TICKS, () -> {
			check(helper, "crossing", true, shield.hitPoint != null && shield.hitPoint.distanceTo(point) < CLOSE);
			check(helper, "one projectile hit", 1, shield.hits);
			check(helper, "HP taken", shield.maxHp() - shield.hp(), shield.hpTaken);
			helper.succeed();
		});
	}

	@GameTest
	public void explosionsReportTheNearestPointOnTheDisc(GameTestHelper helper) {
		TestShields.TestShield shield = shield(helper, TestShields.DISC, RANK);
		Vec3 point = shield.position().add(OFF_CENTRE);
		Vec3 blast = point.add(0, 0, EXPLOSION_DISTANCE);
		helper.getLevel().explode(null, blast.x, blast.y, blast.z, EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
		check(helper, "nearest point", point, shield.hitPoint);
		check(helper, "one blast hit", 1, shield.hits);
		check(helper, "HP taken", shield.maxHp() - shield.hp(), shield.hpTaken);
		helper.succeed();
	}

	@GameTest
	public void aRankBreakReportsAllTheRemainingHpBeforeRemoval(GameTestHelper helper) {
		TestShields.TestShield shield = shield(helper, TestShields.DISC, RANK);
		Arrow attack = helper.spawn(EntityType.ARROW, IN_FRONT);
		attack.setAttached(Attacks.ATTACK, new AttackData(attack.getUUID(), Rank.parse("EX"), false, true));
		shield.hurtServer(helper.getLevel(), helper.getLevel().damageSources().arrow(attack, null), 1.0F);
		check(helper, "all HP taken", shield.maxHp(), shield.hpTaken);
		check(helper, "one hit before removal", 1, shield.hits);
		check(helper, "removed", true, shield.isRemoved());
		helper.succeed();
	}

	@GameTest(maxTicks = TRACKING_TICKS + ARROW_TICKS)
	public void hpTakingHitsAreSentToEveryPlayerTrackingIt(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		ServerPlayer first = watcher(helper, BEHIND);
		ServerPlayer second = watcher(helper, BEHIND.east());
		List<ShieldHitPayload> firstHits = hits(first);
		List<ShieldHitPayload> secondHits = hits(second);
		Vec3 point = shield.position().add(OFF_CENTRE);
		helper.runAfterDelay(TRACKING_TICKS, () -> {
			check(helper, "both tracking", true, PlayerLookup.tracking(shield).containsAll(List.of(first, second)));
			shootNorth(helper, point.add(0, 0, 1));
		});
		helper.runAfterDelay(TRACKING_TICKS + 2, () -> {
			List<ShieldHitPayload> received = hitsOn(firstHits, shield);
			check(helper, "one hit each", true, received.size() == 1 && hitsOn(secondHits, shield).equals(received));
			ShieldHitPayload hit = received.getFirst();
			check(helper, "exact crossing", true, hit.point().distanceTo(point) < CLOSE);
			check(helper, "HP taken", shield.maxHp() - shield.hp(), hit.hpTaken());
			ByteBuf buf = Unpooled.buffer();
			try {
				ShieldHitPayload.STREAM_CODEC.encode(buf, hit);
				check(helper, "payload round-trip", hit, ShieldHitPayload.STREAM_CODEC.decode(buf));
			} finally {
				buf.release();
			}
			shield.hurtAt(helper.getLevel(), helper.getLevel().damageSources().mobAttack(mob(helper, IN_FRONT)), 0.0F, point);
			check(helper, "no HP taken, nothing sent", 1, hitsOn(firstHits, shield).size());
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
