package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.cwzmorro.alayacore.combat.HitCooldown;
import io.github.cwzmorro.alayacore.progression.Progression;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerMeleeMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void alayacore$servantMelee(CallbackInfo ci) {
		Progression.refreshMelee((Player) (Object) this);
	}

	@WrapOperation(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean alayacore$meleeHit(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
		return HitCooldown.melee((Player) (Object) this, target, () -> original.call(target, source, damage));
	}

	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean alayacore$sweepHit(LivingEntity target, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		return HitCooldown.melee((Player) (Object) this, target, () -> original.call(target, level, source, damage));
	}
}
