package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Checks the final teleport position rather than movement, so passengers and position-set teleport paths count too.
@Mixin(Entity.class)
abstract class MarbleTeleportMixin {
	@Inject(method = "teleportSetPosition", at = @At("RETURN"))
	private void alayacore$marbleDisplaced(CallbackInfo ci) {
		if ((Object) this instanceof LivingEntity entity) {
			RealityMarbles.displaced(entity);
		}
	}
}
