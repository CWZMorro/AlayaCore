package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.item.WeaponRules;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fabric has no inventory-drop event; kept weapons must be removed before vanilla drops them.
@Mixin(Inventory.class)
abstract class KeptOnDeathMixin {
	@Inject(method = "dropAll", at = @At("HEAD"))
	private void alayacore$keepWeapons(CallbackInfo ci) {
		WeaponRules.keepOnDeath((Inventory) (Object) this);
	}
}
