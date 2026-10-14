package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.INSIDE;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.advancement;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.award;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.revoke;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mana.CakeFoods;
import io.github.cwzmorro.alayacore.mana.ManaConfig;
import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.mana.SweetFoods;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
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

public class ManaGameTests {
	// Nutrition of one cake slice, as vanilla's CakeBlock gives it.
	private static final int CAKE_SLICE_NUTRITION = 2;
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

	private static AdvancementHolder vanillaAdvancement(GameTestHelper helper, String path) {
		return advancement(helper, Identifier.withDefaultNamespace(path));
	}

	private static void checkFood(GameTestHelper helper, Item food) {
		ServerPlayer player = join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		ItemStack stack = new ItemStack(food);
		FoodProperties properties = stack.get(DataComponents.FOOD);
		stack.finishUsingItem(helper.getLevel(), player);
		double expected = config().startMax() * SweetFoods.foodFraction(helper.getLevel().registryAccess(), properties.nutrition(), BuiltInRegistries.ITEM.getKey(food));
		check(helper, "current", expected, mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void joiningGivesStartMana(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		check(helper, "current", config().startMax(), mana(helper, player).current());
		check(helper, "max", config().startMax(), mana(helper, player).max());
		helper.succeed();
	}

	@GameTest
	public void killingAMobRaisesMaxOnly(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		double current = config().startMax() / 2;
		AlayaMana.update(player, m -> m.withCurrent(current));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, INSIDE);
		zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
		if (zombie.isAlive()) {
			throw helper.assertionException(Component.literal("zombie survived"));
		}
		check(helper, "max", config().startMax() + config().killMaxManaGain(zombie.getMaxHealth()), mana(helper, player).max());
		check(helper, "a tenth of the victim's max HP", config().startMax() + zombie.getMaxHealth() / 10, mana(helper, player).max());
		check(helper, "current", current, mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void everyGrowthSourceStopsAtTheCeiling(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		double nearTop = config().maxCeiling() - 1;
		AlayaMana.update(player, m -> m.withMax(nearTop));
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, INSIDE);
		zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
		check(helper, "a kill stops at the ceiling", config().maxCeiling(), mana(helper, player).max());
		award(player, vanillaAdvancement(helper, "adventure/arbalistic"));
		check(helper, "an advancement stops at the ceiling", config().maxCeiling(), mana(helper, player).max());
		command(helper, "alayacore mana " + player.getUUID() + " max add " + COMMAND_MAX);
		check(helper, "the command stops at the ceiling", config().maxCeiling(), mana(helper, player).max());
		helper.succeed();
	}

	@GameTest
	public void cookieGivesNutritionPlusSweetBonus(GameTestHelper helper) {
		check(helper, "default cookie data", 3.0, helper.getLevel().registryAccess().lookupOrThrow(SweetFoods.KEY)
			.getOptional(BuiltInRegistries.ITEM.getKey(Items.COOKIE)).orElseThrow().percent());
		checkFood(helper, Items.COOKIE);
	}

	@GameTest
	public void testFoodGetsItsBonusFromData(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		ItemStack apple = new ItemStack(Items.APPLE);
		int nutrition = apple.get(DataComponents.FOOD).nutrition();
		apple.finishUsingItem(helper.getLevel(), player);
		check(helper, "data percentage", 7.0, helper.getLevel().registryAccess().lookupOrThrow(SweetFoods.KEY)
			.getOptional(BuiltInRegistries.ITEM.getKey(Items.APPLE)).orElseThrow().percent());
		check(helper, "nutrition plus test data bonus", config().startMax() * config().foodFraction(nutrition,
			BuiltInRegistries.ITEM.getKey(Items.APPLE), 7.0), mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void steakGivesNutritionOnly(GameTestHelper helper) {
		checkFood(helper, Items.COOKED_BEEF);
	}

	@GameTest
	public void cakeSliceGivesNutritionPlusSweetBonus(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		helper.setBlock(INSIDE, Blocks.CAKE);
		helper.useBlock(INSIDE, player);
		check(helper, "default cake slice data", 5.0, SweetFoods.percent(helper.getLevel().registryAccess(), CakeFoods.SLICE));
		double expected = config().startMax() * CakeFoods.foodFraction(helper.getLevel().registryAccess(), CAKE_SLICE_NUTRITION, BuiltInRegistries.ITEM.getKey(Items.CAKE));
		check(helper, "current", expected, mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void taggedModCakeGetsTheFullSliceBonusEvenAtNineteenHunger(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		player.getFoodData().setFoodLevel(19);
		helper.setBlock(INSIDE, TestFoods.CAKE);
		helper.useBlock(INSIDE, player);
		check(helper, "nutrition capped at twenty", 20, player.getFoodData().getFoodLevel());
		check(helper, "actual two-point slice plus cake bonus", config().startMax() * config().foodFraction(CAKE_SLICE_NUTRITION,
			BuiltInRegistries.ITEM.getKey(TestFoods.CAKE.asItem()), 5.0), mana(helper, player).current());
		AlayaMana.update(player, m -> m.withCurrent(0));
		player.getFoodData().setFoodLevel(19);
		helper.setBlock(INSIDE, TestFoods.UNTAGGED);
		helper.useBlock(INSIDE, player);
		check(helper, "untagged block cannot inherit the scoped cake bonus", 0.0, mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void candleCakeGivesOneSliceBonus(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		helper.setBlock(INSIDE, Blocks.CANDLE_CAKE);
		helper.useBlock(INSIDE, player);
		check(helper, "one candle cake slice", config().startMax() * config().foodFraction(CAKE_SLICE_NUTRITION,
			BuiltInRegistries.ITEM.getKey(Items.CAKE), 5.0), mana(helper, player).current());
		helper.succeed();
	}

	@GameTest
	public void challengeAdvancementPaysOnce(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		AdvancementHolder arbalistic = vanillaAdvancement(helper, "adventure/arbalistic");
		double expected = config().startMax() + config().challengeBonus(arbalistic.value().rewards().experience());

		award(player, arbalistic);
		check(helper, "max after first award", expected, mana(helper, player).max());

		revoke(player, arbalistic);
		award(player, arbalistic);
		check(helper, "max after revoke + award", expected, mana(helper, player).max());
		helper.succeed();
	}

	@GameTest
	public void taskAdvancementGivesNothing(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		award(player, vanillaAdvancement(helper, "story/mine_stone"));
		check(helper, "max", config().startMax(), mana(helper, player).max());
		helper.succeed();
	}

	@GameTest
	public void respawnAfterDeathKeepsMaxAndHalvesCurrent(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		double max = config().startMax() * 4;
		AlayaMana.update(player, m -> new ManaData(max, max));
		ServerPlayer respawned = helper.getLevel().getServer().getPlayerList()
			.respawn(player, false, Entity.RemovalReason.KILLED);
		check(helper, "max", max, mana(helper, respawned).max());
		check(helper, "current", config().afterRespawn(new ManaData(max, max)).current(), mana(helper, respawned).current());
		helper.succeed();
	}

	@GameTest(maxTicks = 2 * SharedConstants.TICKS_PER_SECOND)
	public void regeneratesPerSecond(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		AlayaMana.update(player, m -> m.withCurrent(0));
		helper.runAfterDelay(SharedConstants.TICKS_PER_SECOND, () -> {
			check(helper, "current after one second", config().regenPerSecond(), mana(helper, player).current());
			helper.succeed();
		});
	}

	@GameTest
	public void theManaCommandSetsAndAddsCurrentAndMax(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		String target = "alayacore mana " + player.getUUID();
		command(helper, target + " max set " + COMMAND_MAX);
		check(helper, "max set", COMMAND_MAX, mana(helper, player).max());
		command(helper, target + " current set max");
		check(helper, "current filled", COMMAND_MAX, mana(helper, player).current());
		command(helper, target + " max add -" + COMMAND_MAX / 2);
		check(helper, "max lowered", COMMAND_MAX / 2, mana(helper, player).max());
		check(helper, "current kept under it", COMMAND_MAX / 2, mana(helper, player).current());
		command(helper, target + " current add -" + COMMAND_MAX);
		check(helper, "current not below 0", 0.0, mana(helper, player).current());
		helper.succeed();
	}

	private static void command(GameTestHelper helper, String command) {
		helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack(), command);
	}
}
