package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.mana.ManaEvents;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks advancements completing and being revoked: max mana for challenge advancements (plan 7) and the
 * class grade badge (plan 13.4.1), which also follows grades changed by a command.
 */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	/** The moment vanilla hands out an advancement's own rewards: once, when it becomes complete. */
	@Inject(
		method = "award",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V")
	)
	private void alayacore$onCompleted(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		ManaEvents.onAdvancementDone(this.player, holder);
		ClassGrades.onAdvancementChanged(this.player, holder.id());
	}

	@Inject(method = "revoke", at = @At("RETURN"))
	private void alayacore$onRevoked(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			ClassGrades.onAdvancementChanged(this.player, holder.id());
		}
	}
}
