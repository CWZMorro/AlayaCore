package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAsHuman;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.gametest.TestMarbles.TestMarble;
import io.github.cwzmorro.alayacore.marble.RealityMarble;
import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RealityMarbleGameTests {
	private static final Vec3 CENTRE = new Vec3(4.5, 3.5, 4.5);
	private static final double RADIUS = 2.5;
	private static final Vec3 ASIDE = new Vec3(0.5, 1.0, 0.5);
	// Blocks above the centre's block: the column of blocks whose corner lies in the sphere.
	private static final int LOWEST_INSIDE = -1;
	private static final int HIGHEST_INSIDE = 2;
	private static final double ATTACK_REACH = 4;
	private static final double ABOVE = 6;
	private static final int CLOSING_TICKS = 3;
	private static final double EPS = 1e-6;
	private static final float BLAST_POWER = 4;

	private static void check(GameTestHelper helper, boolean condition, String what) {
		TestPlayers.check(helper, what, true, condition);
	}

	private static ServerPlayer caster(GameTestHelper helper, double mana) {
		ServerPlayer player = joinAsHuman(helper);
		player.setGameMode(GameType.SURVIVAL);
		AlayaMana.getOrCreate(player);
		AlayaMana.addMax(player, mana);
		AlayaMana.update(player, data -> data.withCurrent(mana));
		player.snapTo(helper.absoluteVec(CENTRE), 0.0F, 0.0F);
		return player;
	}

	private static TestMarble open(GameTestHelper helper, ServerPlayer caster) {
		TestMarble marble = TestMarbles.open(helper.getLevel(), caster, helper.absoluteVec(CENTRE), RADIUS).marble();
		check(helper, marble != null, "the marble opens where none stands");
		return marble;
	}

	private static BlockPos column(GameTestHelper helper, int up) {
		return BlockPos.containing(helper.absoluteVec(CENTRE)).above(up);
	}

	@GameTest
	public void itsTerrainCannotBeBrokenOrBuiltOn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = caster(helper, 0);
		TestMarble marble = open(helper, caster);
		caster.snapTo(helper.absoluteVec(ASIDE), 0.0F, 0.0F);
		BlockPos inside = column(helper, HIGHEST_INSIDE);
		BlockPos outside = column(helper, HIGHEST_INSIDE + 1);
		check(helper, RealityMarbles.protects(level, column(helper, LOWEST_INSIDE)) && RealityMarbles.protects(level, inside)
			&& !RealityMarbles.protects(level, column(helper, LOWEST_INSIDE - 1)) && !RealityMarbles.protects(level, outside),
			"only the marble's terrain is protected");
		level.setBlock(inside, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(outside, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		check(helper, !caster.gameMode.destroyBlock(inside) && level.getBlockState(inside).is(Blocks.STONE), "a player can't break its terrain");
		check(helper, caster.gameMode.destroyBlock(outside) && level.getBlockState(outside).isAir(), "a player breaks a block outside");
		BlockPos below = column(helper, LOWEST_INSIDE - 1);
		BlockPos support = below.below();
		level.setBlock(support, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		BlockPlaceContext door = new BlockPlaceContext(caster, InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_DOOR),
			new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
		check(helper, door.getClickedPos().equals(below) && RealityMarbles.refusesPlacement(door, Blocks.OAK_DOOR.defaultBlockState()),
			"a door whose upper half would stand in the marble is refused");
		check(helper, !RealityMarbles.refusesPlacement(door, Blocks.STONE.defaultBlockState()), "a single block outside is placed");
		level.setBlock(inside.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(inside, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		ItemStack stone = new ItemStack(Items.STONE, 2);
		caster.setItemInHand(InteractionHand.MAIN_HAND, stone);
		stone.useOn(new UseOnContext(caster, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(inside.below()), Direction.UP, inside.below(), false)));
		check(helper, level.getBlockState(inside).isAir() && stone.getCount() == 2, "placing on its terrain places nothing and uses nothing up");
		marble.close();
		check(helper, !RealityMarbles.protects(level, inside), "a closed marble protects nothing");
		helper.succeed();
	}

	@GameTest
	public void explosionsLeaveItsTerrain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = caster(helper, 0);
		caster.setGameMode(GameType.CREATIVE);
		TestMarble marble = open(helper, caster);
		BlockPos inside = column(helper, HIGHEST_INSIDE);
		BlockPos outside = column(helper, HIGHEST_INSIDE + 2);
		level.setBlock(inside, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(outside, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
		Vec3 blast = Vec3.atCenterOf(inside.above());
		level.explode(null, blast.x, blast.y, blast.z, BLAST_POWER, Level.ExplosionInteraction.TNT);
		check(helper, level.getBlockState(inside).is(Blocks.STONE), "the blast leaves the marble's terrain");
		check(helper, level.getBlockState(outside).isAir(), "the blast breaks the block outside");
		marble.close();
		helper.succeed();
	}

	private static boolean spawns(GameTestHelper helper, BlockPos pos, EntitySpawnReason reason) {
		Zombie zombie = EntityType.ZOMBIE.spawn(helper.getLevel(), pos, reason);
		boolean stays = zombie != null && zombie.isAlive() && helper.getLevel().getEntity(zombie.getUUID()) != null;
		if (zombie != null) {
			zombie.discard();
		}
		return stays;
	}

	@GameTest
	public void onlyNaturalSpawnsAreRefusedInside(GameTestHelper helper) {
		TestMarble marble = open(helper, caster(helper, 0));
		BlockPos inside = column(helper, 0);
		BlockPos outside = column(helper, HIGHEST_INSIDE + 2);
		for (EntitySpawnReason natural : List.of(EntitySpawnReason.NATURAL, EntitySpawnReason.CHUNK_GENERATION,
			EntitySpawnReason.STRUCTURE, EntitySpawnReason.PATROL)) {
			check(helper, !spawns(helper, inside, natural), natural + " spawn inside the marble is refused");
			check(helper, spawns(helper, outside, natural), natural + " spawn outside the marble is allowed");
		}
		for (EntitySpawnReason intended : List.of(EntitySpawnReason.COMMAND, EntitySpawnReason.SPAWN_ITEM_USE,
			EntitySpawnReason.MOB_SUMMONED, EntitySpawnReason.SPAWNER, EntitySpawnReason.BREEDING, EntitySpawnReason.CONVERSION)) {
			check(helper, spawns(helper, inside, intended), intended + " spawn inside the marble is allowed");
		}
		check(helper, RealityMarbles.at(helper.getLevel(), helper.absoluteVec(CENTRE)) == marble, "the marble is the one round its centre");
		marble.close();
		check(helper, spawns(helper, inside, EntitySpawnReason.NATURAL), "natural spawns return once the marble is closed");
		helper.succeed();
	}

	@GameTest
	public void moreManaWinsATieKeepsTheIncumbentABrokenMarbleBlocks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 centre = helper.absoluteVec(CENTRE);
		TestMarble incumbent = open(helper, caster(helper, 100));
		ServerPlayer challenger = caster(helper, 100);
		check(helper, TestMarbles.open(level, challenger, centre, RADIUS).verdict() == RealityMarbles.Verdict.LOST && !incumbent.closed,
			"an exact tie leaves the existing marble standing");
		challenger.setGameMode(GameType.CREATIVE);
		AlayaMana.update(challenger, data -> data.withCurrent(50));
		check(helper, TestMarbles.open(level, challenger, centre, RADIUS).verdict() == RealityMarbles.Verdict.LOST && !incumbent.closed,
			"creative's free spending is not strength");
		challenger.setGameMode(GameType.SURVIVAL);
		AlayaMana.update(challenger, data -> data.withCurrent(101));
		TestMarbles.Opening won = TestMarbles.open(level, challenger, centre.add(RADIUS, 0, 0), RADIUS);
		check(helper, won.verdict() == RealityMarbles.Verdict.CLEAR && won.marble() != null && incumbent.closed,
			"more mana wins: the overlapped marble closes first");
		won.marble().shatter(centre);
		ServerPlayer third = caster(helper, 1_000);
		check(helper, TestMarbles.open(level, third, centre, RADIUS).verdict() == RealityMarbles.Verdict.BROKEN,
			"a broken marble still crumbling blocks any new one");
		won.marble().close();
		helper.succeed();
	}

	@GameTest
	public void anAntiWorldAttackBreaksEveryMarbleItCrosses(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 centre = helper.absoluteVec(CENTRE);
		TestMarble crossed = open(helper, caster(helper, 0));
		ServerPlayer other = caster(helper, 0);
		TestMarble aside = TestMarbles.open(level, other, centre.add(0, ABOVE, 0), RADIUS).marble();
		check(helper, aside != null, "a marble clear of the first opens");
		RealityMarbles.shatter(level, centre.add(-ATTACK_REACH, 0, 0), centre.add(ATTACK_REACH, 0, 0));
		check(helper, crossed.phase() == RealityMarble.Phase.BROKEN && crossed.impact != null
			&& crossed.impact.distanceTo(centre.add(-RADIUS, 0, 0)) < EPS, "from outside it breaks where it enters the shell");
		check(helper, aside.phase() == RealityMarble.Phase.STANDING && aside.impact == null, "a marble it misses stands");
		Vec3 first = crossed.impact;
		RealityMarbles.shatter(level, centre, centre.add(0, 0, ATTACK_REACH));
		check(helper, crossed.impact == first, "a broken marble is not broken again");
		Vec3 within = centre.add(0, ABOVE, 0);
		RealityMarbles.shatter(level, within, within.add(0, 0, ATTACK_REACH));
		check(helper, aside.impact != null && aside.impact.distanceTo(within.add(0, 0, RADIUS)) < EPS,
			"from inside it breaks where it leaves the shell");
		crossed.close();
		aside.close();
		helper.succeed();
	}

	@GameTest
	public void teleportingOutClosesItWalkingOutDoesNot(GameTestHelper helper) {
		ServerPlayer caster = caster(helper, 0);
		TestMarble marble = open(helper, caster);
		Vec3 centre = helper.absoluteVec(CENTRE);
		caster.teleportTo(centre.x + 1, centre.y, centre.z);
		check(helper, !marble.closed, "a teleport within the marble keeps it");
		caster.setPos(centre.add(RADIUS + 2, 0, 0));
		helper.runAfterDelay(CLOSING_TICKS, () -> {
			check(helper, !marble.closed, "walking out is no forced removal");
			caster.teleportTo(centre.x + RADIUS + 3, centre.y, centre.z);
			check(helper, marble.closed, "a teleport out of the marble closes it at once");
			helper.succeed();
		});
	}

	@GameTest
	public void theCastersDeathClosesIt(GameTestHelper helper) {
		ServerPlayer caster = caster(helper, 0);
		TestMarble marble = open(helper, caster);
		// An undecided test player is protected from damage, so the death itself is called (the same death event a real death fires).
		caster.die(helper.getLevel().damageSources().genericKill());
		closesSoon(helper, marble, "the caster's death");
	}

	@GameTest
	public void theCastersLogoutClosesIt(GameTestHelper helper) {
		ServerPlayer caster = caster(helper, 0);
		TestMarble marble = open(helper, caster);
		caster.connection.disconnect(Component.literal("reality marble logout fixture"));
		closesSoon(helper, marble, "the caster's logout");
	}

	@GameTest
	public void changingServantClosesIt(GameTestHelper helper) {
		ServerPlayer caster = caster(helper, 0);
		caster.setAttached(ServantStorage.CHOICE, ServantChoice.servant(TestServants.SOLO));
		TestMarble marble = open(helper, caster);
		helper.runAfterDelay(CLOSING_TICKS, () -> {
			check(helper, !marble.closed, "the servant who opened it keeps it");
			caster.setAttached(ServantStorage.CHOICE, ServantChoice.servant(TestServants.FORCED));
			closesSoon(helper, marble, "changing servant");
		});
	}

	private static void closesSoon(GameTestHelper helper, TestMarble marble, String why) {
		helper.runAfterDelay(CLOSING_TICKS, () -> {
			check(helper, marble.closed, why + " closes the marble by the end of the tick");
			helper.succeed();
		});
	}
}
