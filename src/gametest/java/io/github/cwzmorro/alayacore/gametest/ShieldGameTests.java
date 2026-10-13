package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAsHuman;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.network.ShieldHitPayload;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.shield.ClientShieldEvents;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import io.github.cwzmorro.alayacore.shield.Shields;
import io.github.cwzmorro.alayacore.shield.SolidShields;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
	private static final Vec3 THROUGH = new Vec3(0, 0, -4);
	private static final double OVERLAP = 0.1;
	private static final int PUSH_TICKS = 5;

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
		ServerPlayer player = joinAsHuman(helper);
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
		// A shield takes each attack once a tick: the second hit comes on the next.
		helper.runAfterDelay(1, () -> {
			attacker.setXRot(LOOKING_STRAIGHT_UP);
			hit(helper, attacker, shield);
			check(helper, "fallback to centre", shield.position(), shield.hitPoint);
			hit(helper, mob(helper, BEHIND), shield);
			hit(helper, attacker, shield, 0.0F);
			check(helper, "no hook for refused or zero damage hits", 2, shield.hits);
			helper.succeed();
		});
	}

	@GameTest
	public void aShieldTakesEachAttackOnceATickByItsLargestHit(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		Husk attacker = mob(helper, IN_FRONT);
		hit(helper, attacker, mob(helper, BEHIND), HIT);
		hit(helper, attacker, mob(helper, BEHIND.east()), 2 * HIT);
		hit(helper, attacker, mob(helper, BEHIND.west()), HIT);
		check(helper, "the largest of one attack's hits this tick", shield.maxHp() - 2 * HIT, shield.hp());
		hit(helper, mob(helper, IN_FRONT.east()), mob(helper, BEHIND.below()), HIT);
		check(helper, "another attack counts on its own", shield.maxHp() - 3 * HIT, shield.hp());
		helper.runAfterDelay(1, () -> {
			hit(helper, attacker, mob(helper, BEHIND.above()), HIT);
			check(helper, "the next tick it counts again", shield.maxHp() - 4 * HIT, shield.hp());
			helper.succeed();
		});
	}

	@GameTest
	public void aSolidShieldIsInTheWayOfABodyPutIntoItButNotOfItsCaster(GameTestHelper helper) {
		TestShields.TestShield solid = shield(helper, TestShields.SOLID_DISC, RANK);
		Husk body = mob(helper, IN_FRONT);
		Husk caster = mob(helper, IN_FRONT.east());
		solid.caster = caster.getUUID();
		Vec3 centre = helper.absoluteVec(SHIELD);
		AABB onIt = body.getBoundingBox().move(centre.x - body.getX(), 0, centre.z - body.getZ());
		check(helper, "a body put into it", true, SolidShields.collides(body, onIt));
		check(helper, "its caster", false, SolidShields.collides(caster, onIt));
		check(helper, "a body in front of it", false, SolidShields.collides(body, body.getBoundingBox()));
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
	public void aSolidShieldStopsEveryBodyButItsCaster(GameTestHelper helper) {
		TestShields.TestShield solid = shield(helper, TestShields.SOLID_DISC, RANK);
		Husk walker = mob(helper, IN_FRONT);
		Husk caster = mob(helper, IN_FRONT.east());
		solid.caster = caster.getUUID();
		walker.move(MoverType.SELF, THROUGH);
		caster.move(MoverType.SELF, THROUGH);
		double plane = helper.absoluteVec(SHIELD).z;
		check(helper, "a body stops in front of it", true, walker.getBoundingBox().minZ > plane);
		check(helper, "its caster passes", true, caster.getBoundingBox().maxZ < plane);
		helper.succeed();
	}

	@GameTest
	public void aShieldNotDeclaredSolidStopsNoBody(GameTestHelper helper) {
		shield(helper, TestShields.DISC, RANK);
		Husk walker = mob(helper, IN_FRONT);
		walker.move(MoverType.SELF, THROUGH);
		check(helper, "a body passes", true, walker.getBoundingBox().maxZ < helper.absoluteVec(SHIELD).z);
		helper.succeed();
	}

	@GameTest(maxTicks = PUSH_TICKS + 1)
	public void aSolidShieldPushesOutABodyItIsMovedOntoButNotItsCaster(GameTestHelper helper) {
		TestShields.TestShield solid = shield(helper, TestShields.SOLID_DISC, RANK);
		Vec3 centre = helper.absoluteVec(SHIELD);
		Husk body = mob(helper, IN_FRONT);
		body.snapTo(new Vec3(centre.x, body.getY(), centre.z + OVERLAP));
		Husk caster = mob(helper, IN_FRONT.east());
		Vec3 casterAt = new Vec3(caster.getX(), caster.getY(), centre.z + OVERLAP);
		caster.snapTo(casterAt);
		solid.caster = caster.getUUID();
		double floor = body.getY();
		helper.runAfterDelay(PUSH_TICKS, () -> {
			check(helper, "pushed out to the side it was on", true, body.getBoundingBox().minZ > centre.z);
			check(helper, "pushed along the ground", floor, body.getY());
			check(helper, "its caster is not pushed", casterAt, caster.position());
			helper.succeed();
		});
	}

	@GameTest
	public void eachShieldTypeHasItsOwnHitLook(GameTestHelper helper) {
		TestShields.TestShield disc = helper.spawn(TestShields.DISC, SHIELD);
		TestShields.TestShield dome = helper.spawn(TestShields.DOME, SHIELD);
		ClientShieldEvents.HIT.invoker().onHit(disc, disc.position(), HIT);
		ClientShieldEvents.HIT.invoker().onHit(dome, dome.position(), HIT);
		check(helper, "the disc's look drew its hit", 1, disc.looks);
		check(helper, "no other type's hit reached it", 0, dome.looks);
		helper.succeed();
	}

	@GameTest
	public void anAttackIsMeasuredFromWhereItsHitsComeFrom(GameTestHelper helper) {
		ShieldEntity shield = shield(helper, TestShields.DISC, RANK);
		ServerLevel level = helper.getLevel();
		TestShields.RemoteAttack attack = helper.spawn(TestShields.REMOTE_ATTACK, Vec3.atCenterOf(BEHIND.west()));
		attack.from = helper.absoluteVec(Vec3.atCenterOf(IN_FRONT.south()));
		DamageSource source = level.damageSources().source(DamageTypes.GENERIC, attack, null);
		Husk behind = mob(helper, BEHIND);
		Minecart cart = helper.spawn(EntityType.MINECART, Vec3.atBottomCenterOf(BEHIND.east()));
		cart.setNoGravity(true);
		check(helper, "a creature behind is spared", false, Shields.hurt(level, behind, source, HIT));
		check(helper, "and is unhurt", behind.getMaxHealth(), behind.getHealth());
		check(helper, "so is anything else behind", false, Shields.hurt(level, cart, source, HIT));
		check(helper, "and undamaged", 0.0F, cart.getDamage());
		check(helper, "the shield took them, once in the tick", shield.maxHp() - HIT, shield.hp());
		BlockPos blockBehind = helper.absolutePos(BEHIND.below());
		BlockPos blockInFront = helper.absolutePos(IN_FRONT.below());
		check(helper, "only the block in front may break", List.of(blockInFront),
			Shields.unshielded(level, attack, new ArrayList<>(List.of(blockBehind, blockInFront))));
		helper.succeed();
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
