package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mana.ManaConfig;
import io.github.cwzmorro.alayacore.mana.ManaData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Checks that the mana hooks fire inside a running server (plan 7). The numbers themselves are
 * checked against plan.md in {@code ManaConfigTest}; here they come from the loaded config.
 */
public class ManaGameTests {
	/** Nutrition of one cake slice, as vanilla's {@code CakeBlock} gives it. */
	private static final int CAKE_SLICE_NUTRITION = 2;
	private static final BlockPos INSIDE = new BlockPos(1, 1, 1);
	/** Max mana the command test sets. */
	private static final double COMMAND_MAX = 50_000;

	private static ManaConfig config() {
		return AlayaConfig.mana();
	}

	private static ManaData mana(GameTestHelper helper, ServerPlayer player) {
		ManaData mana = AlayaMana.get(player);
		if (mana == null) {
			throw helper.assertionException(Component.literal("player has no mana"));
		}
		return mana;
	}

	private static AdvancementHolder advancement(GameTestHelper helper, String path) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.withDefaultNamespace(path));
		if (holder == null) {
			throw helper.assertionException(Component.literal("advancement " + path + " missing"));
		}
		return holder;
	}

	private static void complete(ServerPlayer player, AdvancementHolder holder) {
		for (String criterion : holder.value().criteria().keySet()) {
			player.getAdvancements().award(holder, criterion);
		}
	}

	/** Eats a food item from empty mana and checks the gain. */
	private static void checkFood(GameTestHelper helper, Item food) {
		ServerPlayer player = TestPlayers.join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		ItemStack stack = new ItemStack(food);
		FoodProperties properties = stack.get(DataComponents.FOOD);
		stack.finishUsingItem(helper.getLevel(), player);
		double expected = config().startMax() * config().foodFraction(properties.nutrition(), BuiltInRegistries.ITEM.getKey(food));
		TestPlayers.check(helper, "current", expected, mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void joiningGivesStartMana(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		TestPlayers.check(helper, "current", config().startMax(), mana(helper, player).current());
		TestPlayers.check(helper, "max", config().startMax(), mana(helper, player).max());
		helper.succeed();
	}

	@GameTest
	public void killingAMobRaisesMaxOnly(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		double current = config().startMax() / 2;
		AlayaMana.update(player, m -> m.withCurrent(current));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, INSIDE);
		zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
		if (zombie.isAlive()) {
			throw helper.assertionException(Component.literal("zombie survived"));
		}
		TestPlayers.check(helper, "max", config().startMax() + zombie.getMaxHealth(), mana(helper, player).max());
		TestPlayers.check(helper, "current", current, mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void cookieGivesNutritionPlusSweetBonus(GameTestHelper helper) {
		checkFood(helper, Items.COOKIE);
	}

	@GameTest
	public void steakGivesNutritionOnly(GameTestHelper helper) {
		checkFood(helper, Items.COOKED_BEEF);
	}

	@GameTest
	public void cakeSliceGivesNutritionPlusSweetBonus(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		helper.setBlock(INSIDE, Blocks.CAKE);
		helper.useBlock(INSIDE, player);
		double expected = config().startMax() * config().foodFraction(CAKE_SLICE_NUTRITION, BuiltInRegistries.ITEM.getKey(Items.CAKE));
		TestPlayers.check(helper, "current", expected, mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void challengeAdvancementPaysOnce(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		AdvancementHolder arbalistic = advancement(helper, "adventure/arbalistic");
		double expected = config().startMax() + config().challengeBonus(arbalistic.value().rewards().experience());

		complete(player, arbalistic);
		TestPlayers.check(helper, "max after first award", expected, mana(helper, player).max());

		for (String criterion : arbalistic.value().criteria().keySet()) {
			player.getAdvancements().revoke(arbalistic, criterion);
		}
		complete(player, arbalistic);
		TestPlayers.check(helper, "max after revoke + award", expected, mana(helper, player).max());
		helper.succeed();
	}

	@GameTest
	public void taskAdvancementGivesNothing(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		complete(player, advancement(helper, "story/mine_stone"));
		TestPlayers.check(helper, "max", config().startMax(), mana(helper, player).max());
		helper.succeed();
	}

	@GameTest
	public void respawnAfterDeathKeepsMaxAndHalvesCurrent(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		double max = config().startMax() * 4;
		AlayaMana.update(player, m -> new ManaData(max, max));
		ServerPlayer respawned = helper.getLevel().getServer().getPlayerList()
			.respawn(player, false, Entity.RemovalReason.KILLED);
		TestPlayers.check(helper, "max", max, mana(helper, respawned).max());
		TestPlayers.check(helper, "current", config().afterRespawn(new ManaData(max, max)).current(), mana(helper, respawned).current());
		helper.succeed();
	}

	@GameTest(maxTicks = 2 * SharedConstants.TICKS_PER_SECOND)
	public void regeneratesPerSecond(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		helper.runAfterDelay(SharedConstants.TICKS_PER_SECOND, () -> {
			TestPlayers.check(helper, "current after one second", config().regenPerSecond(), mana(helper, player).current());
			helper.succeed();
		});
	}

	@GameTest
	public void theManaCommandSetsAndAddsCurrentAndMax(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper);
		String target = "alayacore mana " + player.getUUID();
		command(helper, target + " max set " + COMMAND_MAX);
		TestPlayers.check(helper, "max set", COMMAND_MAX, mana(helper, player).max());
		command(helper, target + " current set max");
		TestPlayers.check(helper, "current filled", COMMAND_MAX, mana(helper, player).current());
		command(helper, target + " max add -" + COMMAND_MAX / 2);
		TestPlayers.check(helper, "max lowered", COMMAND_MAX / 2, mana(helper, player).max());
		TestPlayers.check(helper, "current kept under it", COMMAND_MAX / 2, mana(helper, player).current());
		command(helper, target + " current add -" + COMMAND_MAX);
		TestPlayers.check(helper, "current not below 0", 0.0, mana(helper, player).current());
		helper.succeed();
	}

	private static void command(GameTestHelper helper, String command) {
		helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack(), command);
	}
}
