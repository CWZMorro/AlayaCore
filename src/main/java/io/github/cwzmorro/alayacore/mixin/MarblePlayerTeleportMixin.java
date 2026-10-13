package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// ServerPlayer overrides Entity's teleport methods; check only successful final positions.
@Mixin(ServerPlayer.class)
abstract class MarblePlayerTeleportMixin {
	@Inject(method = "teleportTo(DDD)V", at = @At("RETURN"))
	private void alayacore$marblePosition(CallbackInfo ci) {
		RealityMarbles.displaced((ServerPlayer) (Object) this);
	}

	@Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At("RETURN"))
	private void alayacore$marbleDimension(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) {
			RealityMarbles.displaced((ServerPlayer) (Object) this);
		}
	}

	@Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("RETURN"))
	private void alayacore$marbleTransition(CallbackInfoReturnable<ServerPlayer> cir) {
		if (cir.getReturnValue() != null) {
			RealityMarbles.displaced(cir.getReturnValue());
		}
	}
}
