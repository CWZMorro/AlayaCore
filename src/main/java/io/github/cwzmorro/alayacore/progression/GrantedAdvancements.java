package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

/** Advancements the mod grants and revokes itself (true name, class grades): all their criteria at once. */
final class GrantedAdvancements {
	private GrantedAdvancements() {
	}

	static boolean isDone(ServerPlayer player, Identifier advancement) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	static void grant(ServerPlayer player, Identifier advancement) {
		forEachCriterion(player, advancement, PlayerAdvancements::award);
	}

	static void revoke(ServerPlayer player, Identifier advancement) {
		forEachCriterion(player, advancement, PlayerAdvancements::revoke);
	}

	private interface CriterionChange {
		boolean apply(PlayerAdvancements advancements, AdvancementHolder holder, String criterion);
	}

	private static void forEachCriterion(ServerPlayer player, Identifier advancement, CriterionChange change) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
		if (holder == null) {
			AlayaCore.LOGGER.warn("Advancement {} does not exist", advancement);
			return;
		}
		holder.value().criteria().keySet().forEach(criterion -> change.apply(player.getAdvancements(), holder, criterion));
	}
}
