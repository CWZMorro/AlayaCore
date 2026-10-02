package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.progression.TrueName;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Raid wins for true-name deeds (plan 13.3). Vanilla fires its "hero of the village" trigger for
 * every hero of a won raid; several triggers share this class, so only that one is passed on.
 */
@Mixin(PlayerTrigger.class)
abstract class RaidWinTriggerMixin {
	@Inject(method = "trigger", at = @At("HEAD"))
	private void alayacore$raidWon(ServerPlayer player, CallbackInfo ci) {
		if ((Object) this == CriteriaTriggers.RAID_WIN) {
			TrueName.onRaidWon(player);
		}
	}
}
