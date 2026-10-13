package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;

import io.github.cwzmorro.alayacore.ability.SkillEffects;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SkillEffectsGameTests {
	private static final Identifier SKILL = TestServants.id("effect_skill");
	private static final Identifier MODIFIER = TestServants.id("effect_boost");
	private static final int POTION_TICKS = 6000;
	private static final int SKILL_TICKS = 200;
	private static final double BONUS = 0.5;

	@GameTest
	public void aSkillTakesBackOnlyItsOwnEffectAndLeavesAPotionsAlone(GameTestHelper helper) {
		LivingEntity entity = helper.spawn(EntityType.COW, TestPlayers.INSIDE);
		entity.addEffect(new MobEffectInstance(MobEffects.SPEED, POTION_TICKS, 0));
		SkillEffects.give(entity, SKILL, new MobEffectInstance(MobEffects.SPEED, SKILL_TICKS, 1));
		check(helper, "the skill's stronger effect is on top", 1, entity.getEffect(MobEffects.SPEED).getAmplifier());
		SkillEffects.take(entity, SKILL, MobEffects.SPEED);
		MobEffectInstance left = entity.getEffect(MobEffects.SPEED);
		check(helper, "the potion's effect is back, untouched", true, left != null && left.getAmplifier() == 0 && left.getDuration() > POTION_TICKS - SKILL_TICKS);
		SkillEffects.take(entity, SKILL, MobEffects.SPEED);
		check(helper, "taking again does nothing", true, entity.getEffect(MobEffects.SPEED) == left);
		helper.succeed();
	}

	@GameTest
	public void aSkillTakesBackAnAttributeItBoosted(GameTestHelper helper) {
		LivingEntity entity = helper.spawn(EntityType.COW, TestPlayers.INSIDE);
		double before = entity.getAttributeValue(Attributes.MOVEMENT_SPEED);
		SkillEffects.boost(entity, Attributes.MOVEMENT_SPEED, MODIFIER, BONUS);
		check(helper, "boosted", before * (1 + BONUS), entity.getAttributeValue(Attributes.MOVEMENT_SPEED));
		SkillEffects.unboost(entity, Attributes.MOVEMENT_SPEED, MODIFIER);
		check(helper, "back as it was", before, entity.getAttributeValue(Attributes.MOVEMENT_SPEED));
		helper.succeed();
	}
}
