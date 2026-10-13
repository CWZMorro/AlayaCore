package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.progression.DeedKinds;
import io.github.cwzmorro.alayacore.progression.TrueName;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Several criteria use PlayerTrigger; only RAID_WIN represents a won raid.
@Mixin(PlayerTrigger.class)
abstract class RaidWinTriggerMixin {
	@Inject(method = "trigger", at = @At("HEAD"))
	private void alayacore$raidWon(ServerPlayer player, CallbackInfo ci) {
		if ((Object) this == CriteriaTriggers.RAID_WIN) {
			TrueName.happened(player, DeedKinds.WIN_RAID, Unit.INSTANCE);
		}
	}
}
