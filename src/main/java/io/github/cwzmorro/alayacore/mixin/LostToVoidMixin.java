package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.item.LostToVoid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fabric has no event for falling out of the world.
@Mixin(Entity.class)
abstract class LostToVoidMixin {
	@Inject(method = "onBelowWorld", at = @At("HEAD"))
	private void alayacore$lostToVoid(CallbackInfo ci) {
		if ((Object) this instanceof ItemEntity item && item.level() instanceof ServerLevel level) {
			LostToVoid.lost(level, item);
		}
	}
}
