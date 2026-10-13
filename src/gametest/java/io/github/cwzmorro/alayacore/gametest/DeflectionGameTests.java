package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;

import io.github.cwzmorro.alayacore.combat.Deflection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.Vec3;

public class DeflectionGameTests {
	private static final double SPEED = 1.5;
	private static final double RADIUS = 3.0;

	private static Arrow flyingAtTheOrigin(GameTestHelper helper) {
		Arrow arrow = helper.spawn(EntityType.ARROW, TestPlayers.INSIDE);
		arrow.setDeltaMovement(0, 0, -SPEED);
		return arrow;
	}

	@GameTest
	public void anOptedInAbilityDeflectsProjectilesFlyingAtItOnce(GameTestHelper helper) {
		ArmorStand source = helper.spawn(EntityType.ARMOR_STAND, TestPlayers.BESIDE);
		Arrow arrow = flyingAtTheOrigin(helper);
		Vec3 point = arrow.position().add(0, 0, -1);
		Set<UUID> already = new HashSet<>();
		Deflection.deflect(helper.getLevel(), TestAbilities.DEFLECTING.identifier(), source, point, point, RADIUS, already);
		check(helper, "pushed away from the point", true, arrow.getDeltaMovement().z > 0);
		check(helper, "at its own speed", SPEED, arrow.getDeltaMovement().length());
		check(helper, "noted as deflected", true, already.contains(arrow.getUUID()));
		arrow.setDeltaMovement(0, 0, -SPEED);
		Deflection.deflect(helper.getLevel(), TestAbilities.DEFLECTING.identifier(), source, point, point, RADIUS, already);
		check(helper, "not deflected twice by the same source", true, arrow.getDeltaMovement().z < 0);
		helper.succeed();
	}

	@GameTest
	public void anAbilityThatDidNotOptInDeflectsNothing(GameTestHelper helper) {
		ArmorStand source = helper.spawn(EntityType.ARMOR_STAND, TestPlayers.BESIDE);
		Arrow arrow = flyingAtTheOrigin(helper);
		Vec3 point = arrow.position().add(0, 0, -1);
		Deflection.deflect(helper.getLevel(), TestAbilities.RECORDER.identifier(), source, point, point, RADIUS, new HashSet<>());
		check(helper, "untouched", true, arrow.getDeltaMovement().z < 0);
		helper.succeed();
	}
}
