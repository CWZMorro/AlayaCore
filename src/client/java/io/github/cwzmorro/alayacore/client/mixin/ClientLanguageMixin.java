package io.github.cwzmorro.alayacore.client.mixin;

import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Reuse ability and mode translations rather than requiring a separate key translation per mode.
@Mixin(ClientLanguage.class)
abstract class ClientLanguageMixin {
	@Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
	private void alayacore$keyName(String key, String defaultValue, CallbackInfoReturnable<String> cir) {
		AbilityKeys.slotName(key).or(() -> AbilityKeys.quickCastName(key)).ifPresent(name -> cir.setReturnValue(name.getString()));
	}
}
