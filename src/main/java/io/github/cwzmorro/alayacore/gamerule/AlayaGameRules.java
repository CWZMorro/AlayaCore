package io.github.cwzmorro.alayacore.gamerule;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/** Alaya Core's game rules ({@code /gamerule alayacore:…}). */
public final class AlayaGameRules {
	public static final GameRule<Boolean> FORCE_RANDOM_SERVANT = GameRuleBuilder.forBoolean(false)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(AlayaCore.id("force_random_servant"));

	public static final GameRule<Integer> GRANDS_PER_CLASS = GameRuleBuilder.forInteger(1)
		.range(1, Integer.MAX_VALUE)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(AlayaCore.id("grands_per_class"));

	public static final GameRule<Boolean> GRAND_TITLE_LOSS_IN_SINGLE_PLAYER = GameRuleBuilder.forBoolean(false)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(AlayaCore.id("grand_title_loss_in_single_player"));

	public static final GameRule<Boolean> WEAPON_BLOCK_DAMAGE = GameRuleBuilder.forBoolean(true)
		.category(GameRuleCategory.MOBS)
		.buildAndRegister(AlayaCore.id("weapon_block_damage"));

	private AlayaGameRules() {
	}

	public static void init() {
	}
}
