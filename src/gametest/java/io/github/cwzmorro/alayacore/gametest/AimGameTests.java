package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;

import io.github.cwzmorro.alayacore.combat.Aim;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

public class AimGameTests {
	private static final BlockPos CASTER = new BlockPos(2, 2, 2);
	private static final BlockPos TARGET = CASTER.east(3);
	private static final double RANGE = 6.0;
	private static final double SHORT_RANGE = 1.0;
	private static final float EAST = -90.0F;
	private static final float WEST = 90.0F;
	private static final double EPS = 1e-6;

	@GameTest
	public void aimPicksTheCreatureInFrontElseTheRangesEnd(GameTestHelper helper) {
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, TARGET);
		zombie.setNoAi(true);
		ServerPlayer caster = join(helper);
		Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(CASTER));
		caster.snapTo(at.x, at.y, at.z, EAST, 0.0F);
		check(helper, "the creature in front", zombie, Aim.target(caster, RANGE));
		check(helper, "its middle", true, Aim.point(caster, RANGE).distanceTo(zombie.getBoundingBox().getCenter()) < EPS);
		caster.snapTo(at.x, at.y, at.z, WEST, 0.0F);
		check(helper, "nothing behind", true, Aim.target(caster, SHORT_RANGE) == null);
		Vec3 end = caster.getEyePosition().add(caster.getLookAngle().scale(SHORT_RANGE));
		check(helper, "the range's end", true, Aim.point(caster, SHORT_RANGE).distanceTo(end) < EPS);
		helper.succeed();
	}
}
