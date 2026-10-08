package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

final class GrantedAdvancements {
	private static final String CRITERION = "granted";

	private GrantedAdvancements() {
	}

	static Advancement.Builder impossible(Advancement.Builder builder) {
		return builder.addCriterion(CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()));
	}

	static boolean isDone(ServerPlayer player, Identifier advancement) {
		AdvancementHolder holder = holder(player, advancement);
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

	private static @Nullable AdvancementHolder holder(ServerPlayer player, Identifier advancement) {
		return player.level().getServer().getAdvancements().get(advancement);
	}

	private static void forEachCriterion(ServerPlayer player, Identifier advancement, CriterionChange change) {
		AdvancementHolder holder = holder(player, advancement);
		if (holder == null) {
			AlayaCore.LOGGER.warn("Advancement {} does not exist", advancement);
			return;
		}
		holder.value().criteria().keySet().forEach(criterion -> change.apply(player.getAdvancements(), holder, criterion));
	}
}
