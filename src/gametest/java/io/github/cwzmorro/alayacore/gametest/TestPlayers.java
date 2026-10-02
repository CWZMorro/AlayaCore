package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.network.ChooseServantPayload;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Helpers shared by the game tests. */
final class TestPlayers {
	/** Health and attributes are 32-bit floats in the game. */
	private static final double EPS = 1e-4;
	/** Client tests: enough for a packet to reach the server and the answer to come back. */
	static final int ROUND_TRIP_TICKS = 2;

	private TestPlayers() {
	}

	/** A fresh player that has joined the server (so join events have run). */
	static ServerPlayer join(GameTestHelper helper) {
		@SuppressWarnings("removal")
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		return player;
	}

	/** A fresh player who chose this servant, freed first in case another test's player holds it. */
	static ServerPlayer joinAs(GameTestHelper helper, Identifier servant) {
		release(helper.getLevel().getServer(), servant);
		ServerPlayer player = join(helper);
		ServantSelection.choose(player, servant(servant));
		return player;
	}

	/** The only player of a client game test's world. */
	static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	static ChooseServantPayload servant(Identifier servant) {
		return new ChooseServantPayload(ChooseServantPayload.Kind.SERVANT, Optional.of(servant));
	}

	static ChooseServantPayload random() {
		return new ChooseServantPayload(ChooseServantPayload.Kind.RANDOM, Optional.empty());
	}

	static ChooseServantPayload human() {
		return new ChooseServantPayload(ChooseServantPayload.Kind.HUMAN, Optional.empty());
	}

	/** Frees test servants that a random pick (or an earlier run: the test world is kept) gave to someone. */
	static void release(MinecraftServer server, Identifier... servants) {
		Map<UUID, Identifier> owners = new HashMap<>(server.globalAttachments().getAttachedOrCreate(ServantStorage.OWNERS));
		owners.values().removeAll(List.of(servants));
		server.globalAttachments().setAttached(ServantStorage.OWNERS, Map.copyOf(owners));
	}

	/** The class's values from the loaded config. */
	static ServantClassConfig classConfig(ResourceKey<ServantClass> key) {
		return AlayaConfig.servants().classConfig(key.identifier(), AlayaRegistries.SERVANT_CLASS.getValueOrThrow(key));
	}

	/** Client tests have no helper: a failed check throws. */
	static void check(boolean condition, String failure) {
		if (!condition) {
			throw new AssertionError(failure);
		}
	}

	static void check(GameTestHelper helper, String what, double expected, double actual) {
		if (Math.abs(expected - actual) > EPS) {
			throw helper.assertionException(Component.literal(what + ": expected " + expected + ", got " + actual));
		}
	}

	static void check(GameTestHelper helper, String what, Object expected, Object actual) {
		if (!expected.equals(actual)) {
			throw helper.assertionException(Component.literal(what + ": expected " + expected + ", got " + actual));
		}
	}
}
