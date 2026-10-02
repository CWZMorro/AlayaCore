package io.github.cwzmorro.alayacore.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** In game, the mode key held + scroll changes the ability preset instead of the hotbar slot (plan 8). */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;"))
	private Vector2i alayacore$scrollPresets(ScrollWheelHandler handler, double scrollX, double scrollY, Operation<Vector2i> original) {
		// A zero result makes vanilla ignore this scroll.
		return AbilityKeys.onScroll(scrollY) ? new Vector2i() : original.call(handler, scrollX, scrollY);
	}
}
