package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.classConfig;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.random;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.release;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.servant;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.network.ChooseServantPayload;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Checks the servant selection flow inside a running server (plan 3). */
public class ServantGameTests {
	/** Which servant each player became, as Alaya Core announced it. */
	private static final Map<UUID, Identifier> BECAME = new ConcurrentHashMap<>();

	static {
		AlayaServants.BECAME_SERVANT.register((player, servant) -> BECAME.put(player.getUUID(), servant));
	}

	private static ServantChoice choice(ServerPlayer player) {
		return player.getAttachedOrCreate(ServantStorage.CHOICE);
	}

	private static boolean choosing(ServerPlayer player) {
		return player.getAttachedOrElse(ServantStorage.CHOOSING, false);
	}

	private static Map<UUID, Identifier> owners(GameTestHelper helper) {
		return helper.getLevel().getServer().globalAttachments().getAttachedOrCreate(ServantStorage.OWNERS);
	}

	private static double speed(ServerPlayer player) {
		return player.getAttributeValue(Attributes.MOVEMENT_SPEED);
	}

	@GameTest
	public void joiningOpensTheScreenAndProtects(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		check(helper, "decided", false, choice(player).decided());
		check(helper, "choosing", true, choosing(player));
		check(helper, "movement speed", 0.0, speed(player));
		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
		check(helper, "alive", true, player.isAlive());
		helper.succeed();
	}

	@GameTest
	public void choosingAServantTakesItAndUnfreezes(GameTestHelper helper) {
		release(helper.getLevel().getServer(), TestServants.SOLO);
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, servant(TestServants.SOLO));
		check(helper, "choice", ServantChoice.servant(TestServants.SOLO), choice(player));
		check(helper, "choosing", false, choosing(player));
		check(helper, "owner", TestServants.SOLO, owners(helper).get(player.getUUID()));
		check(helper, "can move", true, speed(player) > 0.0);
		helper.succeed();
	}

	@GameTest
	public void aTakenServantIsRefused(GameTestHelper helper) {
		if (AlayaConfig.servants().allowDuplicates()) {
			helper.succeed();
			return;
		}
		release(helper.getLevel().getServer(), TestServants.CONTESTED);
		ServerPlayer first = join(helper);
		ServerPlayer second = join(helper);
		ServantSelection.choose(first, servant(TestServants.CONTESTED));
		ServantSelection.choose(second, servant(TestServants.CONTESTED));
		check(helper, "second decided", false, choice(second).decided());
		check(helper, "second still choosing", true, choosing(second));
		helper.succeed();
	}

	@GameTest
	public void stayingHumanDecidesWithoutAServant(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, human());
		check(helper, "choice", ServantChoice.human(), choice(player));
		check(helper, "owner", false, owners(helper).containsKey(player.getUUID()));
		helper.succeed();
	}

	@GameTest
	public void changingServantFreesTheOldOne(GameTestHelper helper) {
		release(helper.getLevel().getServer(), TestServants.OLD, TestServants.NEW);
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, servant(TestServants.OLD));
		ServantSelection.open(player);
		ServantSelection.choose(player, servant(TestServants.NEW));
		check(helper, "owner", TestServants.NEW, owners(helper).get(player.getUUID()));
		check(helper, "old servant owned", false, owners(helper).containsValue(TestServants.OLD));
		helper.succeed();
	}

	@GameTest
	public void becomingAServantIsAnnounced(GameTestHelper helper) {
		release(helper.getLevel().getServer(), TestServants.ANNOUNCED);
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, human());
		check(helper, "not for a human", false, BECAME.containsKey(player.getUUID()));
		ServantSelection.open(player);
		ServantSelection.choose(player, servant(TestServants.ANNOUNCED));
		check(helper, "announced", TestServants.ANNOUNCED, BECAME.get(player.getUUID()));
		helper.succeed();
	}

	@GameTest
	public void anAnswerWithoutTheScreenIsIgnored(GameTestHelper helper) {
		release(helper.getLevel().getServer(), TestServants.IGNORED);
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, human());
		ServantSelection.choose(player, servant(TestServants.IGNORED));
		check(helper, "choice", ServantChoice.human(), choice(player));
		helper.succeed();
	}

	@GameTest
	public void forceRandomRefusesAPickedServant(GameTestHelper helper) {
		release(helper.getLevel().getServer(), TestServants.FORCED);
		MinecraftServer server = helper.getLevel().getServer();
		server.getGameRules().set(AlayaGameRules.FORCE_RANDOM_SERVANT, true, server);
		try {
			ServerPlayer player = join(helper);
			ServantSelection.choose(player, servant(TestServants.FORCED));
			check(helper, "decided after picking", false, choice(player).decided());
			ServantSelection.choose(player, random());
			check(helper, "has a servant after random", true, choice(player).servant().isPresent());
		} finally {
			server.getGameRules().set(AlayaGameRules.FORCE_RANDOM_SERVANT, false, server);
		}
		helper.succeed();
	}

	@GameTest
	public void archerRegeneratesMore(GameTestHelper helper) {
		release(helper.getLevel().getServer(), TestServants.ARCHER);
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, servant(TestServants.ARCHER));
		double multiplier = AlayaServants.manaRegenMultiplier(player);
		check(helper, "archer multiplier", classConfig(ServantClasses.ARCHER).manaRegenMultiplier(), multiplier);

		AlayaMana.update(player, m -> m.withCurrent(0));
		ItemStack cookie = new ItemStack(Items.COOKIE);
		int nutrition = cookie.get(DataComponents.FOOD).nutrition();
		cookie.finishUsingItem(helper.getLevel(), player);
		double expected = AlayaConfig.mana().startMax() * AlayaConfig.mana().foodFraction(nutrition, BuiltInRegistries.ITEM.getKey(Items.COOKIE)) * multiplier;
		check(helper, "current after a cookie", expected, AlayaMana.get(player).current());
		helper.succeed();
	}

	@GameTest
	public void closingTheReopenedBookChangesNothing(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.OLD);
		ServantSelection.open(player);
		ServantSelection.choose(player, new ChooseServantPayload(ChooseServantPayload.Kind.CANCEL, Optional.empty()));
		check(helper, "choice", ServantChoice.servant(TestServants.OLD), choice(player));
		check(helper, "choosing after closing", false, choosing(player));

		ServerPlayer newcomer = join(helper);
		ServantSelection.choose(newcomer, new ChooseServantPayload(ChooseServantPayload.Kind.CANCEL, Optional.empty()));
		check(helper, "first-join screen can't be closed", true, choosing(newcomer));
		helper.succeed();
	}
}
