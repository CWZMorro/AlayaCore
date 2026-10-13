package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.cwzmorro.alayacore.combat.ContinuousSource;
import io.github.cwzmorro.alayacore.combat.HitCooldown;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

// Fabric has no event around the complete hit to restore the cooldown afterwards.
@Mixin(LivingEntity.class)
abstract class ContinuousDamageMixin {
	@WrapMethod(method = "hurtServer")
	private boolean alayacore$continuous(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		if (!(source instanceof ContinuousSource)) {
			return original.call(level, source, damage);
		}
		return HitCooldown.continuous((LivingEntity) (Object) this, () -> original.call(level, source, damage));
	}
}
