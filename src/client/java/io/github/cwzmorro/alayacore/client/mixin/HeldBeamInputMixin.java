package io.github.cwzmorro.alayacore.client.mixin;

import io.github.cwzmorro.alayacore.beam.HeldBeam;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Item-use input scaling leaves jump velocity unchanged.
@Mixin(LocalPlayer.class)
abstract class HeldBeamInputMixin {
	@Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
	private boolean alayacore$heldBeamUsingInput(LocalPlayer player) {
		return player.isUsingItem() || HeldBeam.restrictsMovement(player);
	}

	@Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
	private void alayacore$heldBeamInputScale(CallbackInfoReturnable<Float> cir) {
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (HeldBeam.restrictsMovement(player)) {
			cir.setReturnValue(HeldBeam.movementMultiplier(player));
		}
	}

	@Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
	private void alayacore$heldBeamSlowUse(CallbackInfoReturnable<Boolean> cir) {
		if (HeldBeam.restrictsMovement((LocalPlayer) (Object) this)) {
			cir.setReturnValue(true);
		}
	}
}
