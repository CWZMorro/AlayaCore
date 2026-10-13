package io.github.cwzmorro.alayacore.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The damage a target took in its current hit cooldown, which {@link io.github.cwzmorro.alayacore.combat.HitCooldown} puts back after a hit. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("lastHurt")
	float alayacore$lastHurt();

	@Accessor("lastHurt")
	void alayacore$setLastHurt(float lastHurt);
}
