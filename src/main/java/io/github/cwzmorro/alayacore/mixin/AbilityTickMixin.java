package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.ability.Abilities;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs active ability casts with the entity's own tick, so players and mobs work the same (plan 12). */
@Mixin(LivingEntity.class)
abstract class AbilityTickMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void alayacore$tickAbilities(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide() && Abilities.isCasting(self)) {
			Abilities.tick(self);
		}
	}
}
