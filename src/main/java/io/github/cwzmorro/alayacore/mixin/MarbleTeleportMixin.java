package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// randomTeleport snaps directly to a position, bypassing teleport transitions.
@Mixin(Entity.class)
abstract class MarbleTeleportMixin {
	@Inject(method = "teleportSetPosition", at = @At("RETURN"))
	private void alayacore$marbleDisplaced(CallbackInfo ci) {
		this.alayacore$displaced();
	}

	@Inject(method = "teleportTo(DDD)V", at = @At("RETURN"))
	private void alayacore$marbleSnapped(double x, double y, double z, CallbackInfo ci) {
		this.alayacore$displaced();
	}

	@Unique
	private void alayacore$displaced() {
		if ((Object) this instanceof LivingEntity entity) {
			RealityMarbles.displaced(entity);
		}
	}
}
