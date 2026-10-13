package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;

import io.github.cwzmorro.alayacore.combat.AttackTags;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

public class AttackTagGameTests {
	private static Entity entity(GameTestHelper helper, EntityType<?> type) {
		Entity entity = type.create(helper.getLevel(), EntitySpawnReason.LOAD);
		if (entity == null) {
			throw helper.assertionException(Component.literal("could not create " + type));
		}
		return entity;
	}

	@GameTest
	public void vanillaProjectilesAreDeflectable(GameTestHelper helper) {
		check(helper, "arrow", true, AttackTags.deflectable(entity(helper, EntityType.ARROW)));
		check(helper, "ender pearl", true, AttackTags.deflectable(entity(helper, EntityType.ENDER_PEARL)));
		check(helper, "fishing bobber", true, AttackTags.deflectable(entity(helper, EntityType.FISHING_BOBBER)));
		check(helper, "an entity a mod added to the tag", true, AttackTags.deflectable(entity(helper, EntityType.ARMOR_STAND)));
		check(helper, "a mob is not", false, AttackTags.deflectable(entity(helper, EntityType.ZOMBIE)));
		helper.succeed();
	}

	@GameTest
	public void thrownWeaponsPassThroughEveryShieldAndTaggedEntities(GameTestHelper helper) {
		check(helper, "a disc shield", true, AttackTags.thrownWeaponsPassThrough(entity(helper, TestShields.DISC)));
		check(helper, "a dome shield", true, AttackTags.thrownWeaponsPassThrough(entity(helper, TestShields.DOME)));
		check(helper, "an entity a mod added to the tag", true, AttackTags.thrownWeaponsPassThrough(entity(helper, EntityType.ARMOR_STAND)));
		check(helper, "a mob is struck", false, AttackTags.thrownWeaponsPassThrough(entity(helper, EntityType.ZOMBIE)));
		helper.succeed();
	}
}
