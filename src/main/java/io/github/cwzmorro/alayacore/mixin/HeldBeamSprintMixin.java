package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.beam.HeldBeam;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class HeldBeamSprintMixin {
	@Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
	private void alayacore$heldBeamNoSprint(boolean sprinting, CallbackInfo ci) {
		if (sprinting && HeldBeam.restrictsMovement((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
