package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grand Servant (plan 13.4): the title, its succession and the gamerules. Losing the title also takes
 * the Grand grade (13.4.1). Qualifying (defeating every servant of your class) needs servant NPCs, which
 * come later; they will call {@link #qualify}.
 */
public final class Grand {
	/** Server-wide, saved with the world. */
	public static final AttachmentType<GrandData> DATA = AttachmentRegistry.create(AlayaCore.id("grands"), builder -> builder
		.initializer(() -> GrandData.EMPTY)
		.persistent(GrandData.CODEC));

	private Grand() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				onDeath(player);
			}
		});
	}

	public static boolean isGrand(ServerPlayer player) {
		return AlayaServants.classId(player).map(c -> data(server(player)).isGrand(c, player.getUUID())).orElse(false);
	}

	/** The player defeated every servant of their class. */
	public static void qualify(ServerPlayer player) {
		MinecraftServer server = server(player);
		AlayaServants.classId(player).ifPresent(c -> update(server, data(server).qualify(c, player.getUUID(), grandsPerClass(player))));
	}

	/** Whether every title of the player's class is held by someone else. */
	public static boolean titleTaken(ServerPlayer player) {
		return AlayaServants.classId(player).map(c -> data(server(player)).taken(c, player.getUUID(), grandsPerClass(player))).orElse(false);
	}

	/** The player loses the title, if they hold it; the next in line takes it, as on death. */
	public static void loseTitle(ServerPlayer player) {
		MinecraftServer server = server(player);
		AlayaServants.classId(player)
			.filter(c -> data(server).isGrand(c, player.getUUID()))
			.ifPresent(c -> update(server, data(server).loseTitle(c, player.getUUID(), grandsPerClass(player))));
	}

	private static void onDeath(ServerPlayer player) {
		MinecraftServer server = server(player);
		if (server.isSingleplayer() && !player.level().getGameRules().get(AlayaGameRules.GRAND_TITLE_LOSS_IN_SINGLE_PLAYER)) {
			return;
		}
		loseTitle(player);
	}

	private static int grandsPerClass(ServerPlayer player) {
		return player.level().getGameRules().get(AlayaGameRules.GRANDS_PER_CLASS);
	}

	private static MinecraftServer server(ServerPlayer player) {
		return player.level().getServer();
	}

	private static GrandData data(MinecraftServer server) {
		return server.globalAttachments().getAttachedOrCreate(DATA);
	}

	/** Saves the titles; online players' Grand grades follow (plan 13.4.1). */
	private static void update(MinecraftServer server, GrandData data) {
		server.globalAttachments().setAttached(DATA, data);
		server.getPlayerList().getPlayers().forEach(ClassGrades::refresh);
	}
}
