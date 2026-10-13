package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.cwzmorro.alayacore.combat.AttackDefence;
import io.github.cwzmorro.alayacore.combat.ContinuousSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class AttackDefenceMixin {
	@WrapOperation(
		method = "getDamageAfterArmorAbsorb",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F")
	)
	private float alayacore$armor(LivingEntity victim, float damage, DamageSource source, float armor, float toughness, Operation<Float> original) {
		// Armor takes a smaller share off a bigger hit, so a continuous tick takes the share its whole second would.
		float basis = ContinuousSource.armorBasis(source, damage);
		float afterArmor = AttackDefence.damageAfterArmor(source, basis, original.call(victim, basis, source, AttackDefence.armor(source, armor), toughness));
		return basis == damage ? afterArmor : afterArmor * damage / basis;
	}

	@WrapOperation(
		method = "getDamageAfterArmorAbsorb",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V")
	)
	private void alayacore$armorWear(LivingEntity victim, DamageSource source, float damage, Operation<Void> original) {
		float wear = AttackDefence.wearsArmor(source) ? ContinuousSource.armorWear(victim, source, damage) : 0.0F;
		if (wear > 0) {
			original.call(victim, source, wear);
		}
	}

	@WrapOperation(
		method = "getDamageAfterMagicAbsorb",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F")
	)
	private float alayacore$protection(ServerLevel level, LivingEntity victim, DamageSource source, Operation<Float> original) {
		return AttackDefence.protection(source, original.call(level, victim, source));
	}
}
