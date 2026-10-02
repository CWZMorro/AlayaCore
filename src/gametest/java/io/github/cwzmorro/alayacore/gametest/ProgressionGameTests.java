package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.classConfig;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;

import io.github.cwzmorro.alayacore.AlayaItems;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.progression.ClassGrade;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import io.github.cwzmorro.alayacore.progression.Grand;
import io.github.cwzmorro.alayacore.progression.GrandData;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.progression.ProgressionConfig;
import io.github.cwzmorro.alayacore.progression.TrueName;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/** Checks progression inside a running server (plan 13.2–13.4, 13.7). */
public class ProgressionGameTests {
	private static final BlockPos INSIDE = new BlockPos(1, 1, 1);
	private static final BlockPos BESIDE = new BlockPos(3, 1, 1);
	/** Enough of each grade item for every step and one more. */
	private static final int PROMOTIONS = 4;
	private static final int DEMOTIONS = 5;
	private static final int PROTECTION_LEVEL = 4;
	private static final float HIT = 10.0F;
	/** Vanilla: each point of Protection blocks 1/25 (4%) of the damage. */
	private static final double PROTECTION_POINTS_FOR_ALL = 25.0;

	private static boolean done(GameTestHelper helper, ServerPlayer player, String path) {
		AdvancementHolder holder = helper.getLevel().getServer().getAdvancements().get(Identifier.parse(path));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static void kill(GameTestHelper helper, ServerPlayer killer, LivingEntity victim) {
		victim.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(killer), Float.MAX_VALUE);
	}

	@GameTest
	public void choosingAServantGivesDemiStats(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.SOLO);
		ServantStats demi = AlayaConfig.progression().demi();
		check(helper, "max HP", demi.maxHp(), player.getMaxHealth());
		check(helper, "HP raised with max HP", demi.maxHp(), player.getHealth());
		check(helper, "attack", player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + demi.attack(), player.getAttributeValue(Attributes.ATTACK_DAMAGE));
		check(helper, "armor", demi.armor(), player.getAttributeValue(Attributes.ARMOR));
		helper.succeed();
	}

	@GameTest
	public void moreMaxManaRaisesStats(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.SOLO);
		ProgressionConfig config = AlayaConfig.progression();
		double maxMana = config.syncStartMana() * 10; // sync 50%
		AlayaMana.update(player, m -> m.withMax(maxMana));
		ServantStats expected = ServantStats.between(config.demi(), classConfig(ServantClasses.SABER).real(), config.barFill(config.sync(maxMana), false));
		check(helper, "max HP", expected.maxHp(), player.getMaxHealth());
		check(helper, "HP rose by the same amount", expected.maxHp(), player.getHealth());
		helper.succeed();
	}

	@GameTest
	public void humansHaveNoServantStats(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, human());
		check(helper, "max HP", player.getAttributeBaseValue(Attributes.MAX_HEALTH), player.getMaxHealth());
		helper.succeed();
	}

	@GameTest
	public void deedsThenABossRealizeTheTrueName(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.ARCHER);
		check(helper, "throne of heroes", true, done(helper, player, TrueName.THRONE_OF_HEROES.toString()));
		check(helper, "branch", true, done(helper, player, "alayacore-gametest:archer/root"));

		kill(helper, player, helper.spawn(EntityType.WITHER, BESIDE));
		check(helper, "boss before the deeds does nothing", false, done(helper, player, "alayacore-gametest:archer/true_name"));

		CriteriaTriggers.RAID_WIN.trigger(player);
		check(helper, "raid deed before the kill deed", false, done(helper, player, "alayacore-gametest:archer/win_raid"));
		kill(helper, player, helper.spawn(EntityType.ZOMBIE, INSIDE));
		check(helper, "kill deed", true, done(helper, player, "alayacore-gametest:archer/kill_zombie"));
		CriteriaTriggers.RAID_WIN.trigger(player);
		check(helper, "raid deed", true, done(helper, player, "alayacore-gametest:archer/win_raid"));

		kill(helper, player, helper.spawn(EntityType.WITHER, BESIDE));
		check(helper, "true name", true, done(helper, player, "alayacore-gametest:archer/true_name"));
		// The kills above raised max mana too, so sync is not 0.
		ProgressionConfig config = AlayaConfig.progression();
		check(helper, "max HP with the true name share", ServantStats.between(config.demi(), classConfig(ServantClasses.ARCHER).real(),
			config.barFill(Progression.sync(player), true)).maxHp(),
			player.getMaxHealth());
		helper.succeed();
	}

	@GameTest
	public void servantDamageIgnoresHalfOfProtection(GameTestHelper helper) {
		ServerPlayer human = join(helper);
		ServerPlayer servant = joinAs(helper, TestServants.NEW);
		ItemStack chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);
		chestplate.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), PROTECTION_LEVEL);

		Zombie hitByHuman = helper.spawn(EntityType.ZOMBIE, INSIDE);
		Zombie hitByServant = helper.spawn(EntityType.ZOMBIE, BESIDE);
		hitByHuman.setItemSlot(EquipmentSlot.CHEST, chestplate.copy());
		hitByServant.setItemSlot(EquipmentSlot.CHEST, chestplate.copy());
		float before = hitByHuman.getHealth();
		hitByHuman.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(human), HIT);
		hitByServant.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(servant), HIT);
		double humanDamage = before - hitByHuman.getHealth();
		double servantDamage = before - hitByServant.getHealth();

		// One Protection IV piece = 4 protection points, each 4%: a human meets all 4, a servant half of them.
		double full = 1 - PROTECTION_LEVEL / PROTECTION_POINTS_FOR_ALL;
		double half = 1 - PROTECTION_LEVEL * (1 - Percent.toFraction(AlayaConfig.combat().servantProtectionIgnorePercent())) / PROTECTION_POINTS_FOR_ALL;
		check(helper, "servant vs human damage ratio", half / full, servantDamage / humanDamage);
		helper.succeed();
	}

	@GameTest
	public void aGrandWhoDiesLosesTheTitle(GameTestHelper helper) {
		// The test world is kept between runs: start from no titles.
		helper.getLevel().getServer().globalAttachments().setAttached(Grand.DATA, GrandData.EMPTY);
		ServerPlayer player = joinAs(helper, TestServants.OLD);
		Grand.qualify(player);
		check(helper, "grand after qualifying", true, Grand.isGrand(player));
		// A test player never finishes loading a client, and vanilla protects such players from damage,
		// so the death itself is called (the same death event a real death fires).
		player.die(helper.getLevel().damageSources().genericKill());
		check(helper, "grand after dying (dedicated server)", false, Grand.isGrand(player));
		helper.succeed();
	}

	@GameTest
	public void gradesFollowSyncTrueNameAndTheTitle(GameTestHelper helper) {
		// The test world is kept between runs: start from no titles.
		helper.getLevel().getServer().globalAttachments().setAttached(Grand.DATA, GrandData.EMPTY);
		ServerPlayer player = joinAs(helper, TestServants.ARCHER);
		check(helper, "Classes tab", true, done(helper, player, ClassGrades.ROOT.toString()));
		checkGrade(helper, player, ClassGrade.BLACK);

		ProgressionConfig config = AlayaConfig.progression();
		AlayaMana.update(player, m -> m.withMax(AlayaConfig.progression().manaForSync(Percent.toFraction(config.bronzeSyncPercent()))));
		checkGrade(helper, player, ClassGrade.BRONZE);
		AlayaMana.update(player, m -> m.withMax(AlayaConfig.progression().manaForSync(Percent.toFraction(config.silverSyncPercent()))));
		checkGrade(helper, player, ClassGrade.SILVER);

		kill(helper, player, helper.spawn(EntityType.ZOMBIE, INSIDE));
		CriteriaTriggers.RAID_WIN.trigger(player);
		kill(helper, player, helper.spawn(EntityType.WITHER, BESIDE));
		checkGrade(helper, player, ClassGrade.GOLD);

		Grand.qualify(player);
		checkGrade(helper, player, ClassGrade.GRAND);
		// The test server is dedicated, so dying costs the title, and with it the Grand grade.
		player.die(helper.getLevel().damageSources().genericKill());
		check(helper, "grade after losing the title", ClassGrade.GOLD, player.getAttached(ClassGrades.GRADE));
		check(helper, "Grand advancement after losing the title", false,
			done(helper, player, ClassGrades.advancement(ServantClasses.ARCHER.identifier(), ClassGrade.GRAND).toString()));

		// A revoke (here as the /advancement command does it) moves the badge down too.
		AdvancementHolder gold = helper.getLevel().getServer().getAdvancements()
			.get(ClassGrades.advancement(ServantClasses.ARCHER.identifier(), ClassGrade.GOLD));
		gold.value().criteria().keySet().forEach(criterion -> player.getAdvancements().revoke(gold, criterion));
		check(helper, "grade after revoking Gold", ClassGrade.SILVER, player.getAttached(ClassGrades.GRADE));
		helper.succeed();
	}

	@GameTest
	public void theGradeItemsMoveTheGradeAStepAtATime(GameTestHelper helper) {
		helper.getLevel().getServer().globalAttachments().setAttached(Grand.DATA, GrandData.EMPTY);
		ServerPlayer player = joinAs(helper, TestServants.ARCHER);
		player.setGameMode(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRADE_PROMOTION, PROMOTIONS));
		for (ClassGrade grade : List.of(ClassGrade.BRONZE, ClassGrade.SILVER, ClassGrade.GOLD)) {
			use(player);
			checkGrade(helper, player, grade);
		}
		check(helper, "the true name with Gold", true, TrueName.isRealized(player));
		check(helper, "used up, one a step", PROMOTIONS - 3, player.getMainHandItem().getCount());
		use(player);
		check(helper, "no higher than Gold; not used up", PROMOTIONS - 3, player.getMainHandItem().getCount());

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRAND_PROMOTION));
		use(player);
		checkGrade(helper, player, ClassGrade.GRAND);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRADE_DEMOTION, DEMOTIONS));
		for (ClassGrade grade : List.of(ClassGrade.GOLD, ClassGrade.SILVER, ClassGrade.BRONZE, ClassGrade.BLACK)) {
			use(player);
			check(helper, "demoted", grade, player.getAttached(ClassGrades.GRADE));
		}
		check(helper, "no true name below Gold", false, TrueName.isRealized(player));
		check(helper, "the sync of Black", 0.0, Progression.sync(player));
		use(player);
		check(helper, "no lower than Black; not used up", DEMOTIONS - 4, player.getMainHandItem().getCount());

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRAND_PROMOTION));
		player.setGameMode(GameType.CREATIVE);
		use(player);
		check(helper, "Grand needs Gold, in creative too", ClassGrade.BLACK, player.getAttached(ClassGrades.GRADE));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRADE_PROMOTION));
		use(player);
		check(helper, "not used up in creative", 1, player.getMainHandItem().getCount());
		for (int step = 0; step < 2; step++) {
			use(player);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRAND_PROMOTION));
		use(player);
		check(helper, "Grand from Gold", ClassGrade.GRAND, player.getAttached(ClassGrades.GRADE));

		ServerPlayer other = joinAs(helper, TestServants.ARCHER_TWO);
		other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRAND_PROMOTION));
		use(other);
		check(helper, "the title held by someone else: refused", false, Grand.isGrand(other));
		helper.succeed();
	}

	private static void use(ServerPlayer player) {
		player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
	}

	private static void checkGrade(GameTestHelper helper, ServerPlayer player, ClassGrade expected) {
		check(helper, "badge grade", expected, player.getAttached(ClassGrades.GRADE));
		check(helper, expected.key() + " advancement", true,
			done(helper, player, ClassGrades.advancement(ServantClasses.ARCHER.identifier(), expected).toString()));
	}
}
