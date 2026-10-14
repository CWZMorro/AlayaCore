package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.combat.ForcedAdvance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class ForcedAdvanceMixin {
	@Inject(method = "travel", at = @At("HEAD"))
	private void alayacore$forcedAdvance(Vec3 input, CallbackInfo ci) {
		ForcedAdvance.drive((LivingEntity) (Object) this);
	}
}
