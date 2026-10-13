package io.github.cwzmorro.alayacore.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The weaker or shorter instance vanilla keeps beneath an effect, to be shown again when the one above ends. */
@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
	@Accessor("hiddenEffect")
	MobEffectInstance alayacore$hiddenEffect();
}
