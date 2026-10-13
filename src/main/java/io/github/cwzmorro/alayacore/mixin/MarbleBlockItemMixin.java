package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// canPlace receives the resolved placement position and runs before any item is consumed.
@Mixin(BlockItem.class)
abstract class MarbleBlockItemMixin {
	@Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
	private void alayacore$marblePlacement(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (RealityMarbles.refusesPlacement(context, state)) {
			// Vanilla prediction can shrink the held stack even though the server never changed it.
			// An unchanged authoritative slot is not sent by normal container dirty checking.
			if (context.getPlayer() instanceof ServerPlayer player) {
				player.inventoryMenu.sendAllDataToRemote();
			}
			cir.setReturnValue(false);
		}
	}
}
