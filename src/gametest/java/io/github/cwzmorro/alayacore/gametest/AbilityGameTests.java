package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestAbilities.event;
import static io.github.cwzmorro.alayacore.gametest.TestAbilities.events;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.classConfig;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;

import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityRules;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Checks the ability system inside a running server (plan 8). A test player is never ticked (it has
 * no real connection), so whatever needs ticks — holding, draining — is checked on a zombie made a
 * servant, which also shows mobs use the same system (plan 12). A real player is checked in
 * {@code AbilityClientTest}.
 */
public class AbilityGameTests {
	private static final BlockPos INSIDE = new BlockPos(1, 1, 1);
	private static final int MAX_TICKS = 3 * SharedConstants.TICKS_PER_SECOND;

	private static Identifier id(ResourceKey<Ability> ability) {
		return ability.identifier();
	}

	private static Zombie zombieCaster(GameTestHelper helper) {
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, INSIDE);
		zombie.setAttached(ServantStorage.CHOICE, ServantChoice.servant(TestServants.CASTER));
		AlayaMana.getOrCreate(zombie);
		return zombie;
	}

	private static void press(LivingEntity entity, ResourceKey<Ability> ability) {
		Abilities.press(entity, id(ability), 0, false);
	}

	private static void tap(LivingEntity entity, ResourceKey<Ability> ability) {
		press(entity, ability);
		Abilities.release(entity, id(ability));
	}

	private static boolean started(LivingEntity entity, ResourceKey<Ability> ability) {
		return events(entity).contains(event(ability, "start"));
	}

	@GameTest
	public void aTapKeepsTheRecorderActiveAndASecondPressEndsIt(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		tap(player, TestAbilities.RECORDER);
		check(helper, "after a tap", List.of(event(TestAbilities.RECORDER, "start"), event(TestAbilities.RECORDER, "tap")), events(player));
		check(helper, "casting after a tap", true, Abilities.isCasting(player));
		press(player, TestAbilities.RECORDER);
		check(helper, "second press", List.of(event(TestAbilities.RECORDER, "start"), event(TestAbilities.RECORDER, "tap"),
			event(TestAbilities.RECORDER, "again"), event(TestAbilities.RECORDER, "end")), events(player));
		check(helper, "casting after the second press", false, Abilities.isCasting(player));
		helper.succeed();
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void holdingPastTheThresholdIsAHold(GameTestHelper helper) {
		Zombie zombie = zombieCaster(helper);
		press(zombie, TestAbilities.RECORDER);
		int threshold = AlayaConfig.abilities().holdThresholdTicks();
		// One tick of slack: the zombie may already tick once in the tick the key went down.
		helper.runAfterDelay(threshold - 2, () -> check(helper, "hold before the threshold", false,
			events(zombie).contains(event(TestAbilities.RECORDER, "hold_start"))));
		helper.runAfterDelay(threshold + 1, () -> {
			check(helper, "hold after the threshold", true, events(zombie).contains(event(TestAbilities.RECORDER, "hold_start")));
			check(helper, "held ticks", true, events(zombie).contains(event(TestAbilities.RECORDER, "held")));
			Abilities.release(zombie, id(TestAbilities.RECORDER));
			List<String> events = events(zombie);
			check(helper, "ends with release, end", List.of(event(TestAbilities.RECORDER, "released"), event(TestAbilities.RECORDER, "end")),
				events.subList(events.size() - 2, events.size()));
			check(helper, "casting after the release", false, Abilities.isCasting(zombie));
			helper.succeed();
		});
	}

	@GameTest
	public void abilitiesTheEntityDoesNotHaveAreIgnored(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, human());
		tap(player, TestAbilities.RECORDER);
		check(helper, "casting", false, Abilities.isCasting(player));
		check(helper, "events", List.of(), events(player));
		helper.succeed();
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void aCooldownRefusesUntilItEnds(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		tap(player, TestAbilities.COOLDOWN);
		press(player, TestAbilities.COOLDOWN);
		check(helper, "ended", true, events(player).contains(event(TestAbilities.COOLDOWN, "end")));
		press(player, TestAbilities.COOLDOWN);
		check(helper, "refused on cooldown", false, Abilities.isCasting(player));
		helper.runAfterDelay(AbilityRules.toTicks(TestAbilities.COOLDOWN_SECONDS), () -> {
			press(player, TestAbilities.COOLDOWN);
			check(helper, "starts after the cooldown", true, Abilities.isCasting(player));
			helper.succeed();
		});
	}

	@GameTest(maxTicks = MAX_TICKS)
	public void startCostAndHeldDrainUntilManaRunsOut(GameTestHelper helper) {
		Zombie zombie = zombieCaster(helper);
		double max = AlayaMana.get(zombie).max();
		AlayaMana.update(zombie, m -> new ManaData(TestAbilities.START_COST - 1, max));
		press(zombie, TestAbilities.COSTLY);
		check(helper, "refused without the start cost", false, Abilities.isCasting(zombie));
		check(helper, "mana untouched", TestAbilities.START_COST - 1, AlayaMana.get(zombie).current());

		double leftOver = TestAbilities.HELD_COST / 2;
		AlayaMana.update(zombie, m -> new ManaData(TestAbilities.START_COST + 2 * TestAbilities.HELD_COST + leftOver, max));
		press(zombie, TestAbilities.COSTLY);
		check(helper, "start cost paid", 2 * TestAbilities.HELD_COST + leftOver, AlayaMana.get(zombie).current());
		helper.runAfterDelay(AlayaConfig.abilities().holdThresholdTicks() + 2 + 1, () -> {
			// Two held ticks are paid; the third can't be, so the cast ends with the rest unspent.
			check(helper, "stopped when mana ran out", false, Abilities.isCasting(zombie));
			check(helper, "left over", leftOver, AlayaMana.get(zombie).current());
			helper.succeed();
		});
	}

	@GameTest
	public void anAbilityCanRefuseWithoutACooldown(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		tap(player, TestAbilities.PICKY);
		check(helper, "refused", false, TestAbilities.events(player).contains(TestAbilities.event(TestAbilities.PICKY, "start")));
		check(helper, "no cooldown", 0.0, Abilities.cooldownSecondsLeft(player, TestAbilities.PICKY.identifier(), 0));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		tap(player, TestAbilities.PICKY);
		check(helper, "started", true, TestAbilities.events(player).contains(TestAbilities.event(TestAbilities.PICKY, "start")));
		helper.succeed();
	}

	@GameTest
	public void exclusiveAbilitiesDoNotRunWithOthers(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		tap(player, TestAbilities.RECORDER);
		tap(player, TestAbilities.EXCLUSIVE);
		check(helper, "exclusive refused while another is active", false, started(player, TestAbilities.EXCLUSIVE));
		press(player, TestAbilities.RECORDER);

		tap(player, TestAbilities.EXCLUSIVE);
		check(helper, "exclusive starts alone", true, started(player, TestAbilities.EXCLUSIVE));
		List<String> before = events(player);
		tap(player, TestAbilities.RECORDER);
		check(helper, "nothing joins an exclusive ability", before, events(player));
		helper.succeed();
	}

	@GameTest
	public void aNoblePhantasmNeedsTheTrueName(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		tap(player, TestAbilities.NOBLE_PHANTASM);
		check(helper, "started before true name realization", false, started(player, TestAbilities.NOBLE_PHANTASM));
		helper.succeed();
	}

	@GameTest
	public void castingPausesRegenerationExceptForArchers(GameTestHelper helper) {
		ServerPlayer saber = joinAs(helper, TestServants.CASTER);
		check(helper, "saber before casting", 1.0, AlayaServants.manaRegenMultiplier(saber));
		tap(saber, TestAbilities.RECORDER);
		check(helper, "saber while casting", 0.0, AlayaServants.manaRegenMultiplier(saber));

		ServerPlayer archer = joinAs(helper, TestServants.ARCHER);
		tap(archer, TestAbilities.RECORDER);
		check(helper, "archer while casting", classConfig(ServantClasses.ARCHER).manaRegenMultiplier(), AlayaServants.manaRegenMultiplier(archer));
		helper.succeed();
	}

	@GameTest
	public void deathAndAServantChangeEndCasts(GameTestHelper helper) {
		ServerPlayer dying = joinAs(helper, TestServants.CASTER);
		tap(dying, TestAbilities.RECORDER);
		// A test player is protected from damage (no client), so the death itself is called.
		dying.die(helper.getLevel().damageSources().genericKill());
		check(helper, "ended by death", true, events(dying).contains(event(TestAbilities.RECORDER, "end")));

		ServerPlayer changing = joinAs(helper, TestServants.CASTER);
		tap(changing, TestAbilities.RECORDER);
		ServantSelection.open(changing);
		ServantSelection.choose(changing, human());
		check(helper, "ended by the servant change", true, events(changing).contains(event(TestAbilities.RECORDER, "end")));
		check(helper, "casting", false, Abilities.isCasting(changing));
		helper.succeed();
	}
}
