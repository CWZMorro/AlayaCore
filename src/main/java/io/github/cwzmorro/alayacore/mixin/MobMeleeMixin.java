package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.cwzmorro.alayacore.combat.HitCooldown;
import io.github.cwzmorro.alayacore.progression.Progression;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
abstract class MobMeleeMixin {
	@Inject(method = "doHurtTarget", at = @At("HEAD"))
	private void alayacore$servantMelee(CallbackInfoReturnable<Boolean> cir) {
		Progression.refreshMelee((Mob) (Object) this);
	}

	@WrapOperation(method = "doHurtTarget", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean alayacore$meleeHit(Entity target, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		return HitCooldown.melee((Mob) (Object) this, target, () -> original.call(target, level, source, damage));
	}
}
