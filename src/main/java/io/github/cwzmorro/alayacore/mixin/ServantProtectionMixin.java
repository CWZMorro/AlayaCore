package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** All servant damage ignores part of the target's Protection enchantment (plan 13.7). */
@Mixin(LivingEntity.class)
abstract class ServantProtectionMixin {
	@WrapOperation(
		method = "getDamageAfterMagicAbsorb",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F")
	)
	private float alayacore$servantIgnoresProtection(ServerLevel level, LivingEntity victim, DamageSource source, Operation<Float> original) {
		float protection = original.call(level, victim, source);
		if (source.getEntity() instanceof LivingEntity attacker && AlayaServants.servantId(attacker).isPresent()) {
			return AlayaConfig.combat().protectionAgainstServant(protection);
		}
		return protection;
	}
}
