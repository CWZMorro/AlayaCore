package io.github.cwzmorro.alayacore.gamerule;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/** Alaya Core's game rules ({@code /gamerule alayacore:…}). */
public final class AlayaGameRules {
	/** The selection screen offers only "become a servant (random)" or "human" (plan 3). */
	public static final GameRule<Boolean> FORCE_RANDOM_SERVANT = GameRuleBuilder.forBoolean(false)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(AlayaCore.id("force_random_servant"));

	/** How many players of one class can hold the Grand title at once (plan 13.4). */
	public static final GameRule<Integer> GRANDS_PER_CLASS = GameRuleBuilder.forInteger(1)
		.range(1, Integer.MAX_VALUE)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(AlayaCore.id("grands_per_class"));

	/** Single player: a Grand who dies loses the title and must earn it again (plan 13.4). Off: the title is kept. */
	public static final GameRule<Boolean> GRAND_TITLE_LOSS_IN_SINGLE_PLAYER = GameRuleBuilder.forBoolean(false)
		.category(GameRuleCategory.PLAYER)
		.buildAndRegister(AlayaCore.id("grand_title_loss_in_single_player"));

	private AlayaGameRules() {
	}

	/** Loads this class, which registers the rules. */
	public static void init() {
	}
}
