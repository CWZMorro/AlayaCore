package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.mana.CakeConsumption;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Observe nutrition before hunger capping so a slice at nineteen hunger still counts in full.
@Mixin(FoodData.class)
abstract class CakeFoodDataMixin implements CakeConsumption {
	@Unique
	private @Nullable IntConsumer alayacore$cakeNutrition;

	@Override
	@Unique
	public InteractionResult alayacore$withCake(IntConsumer nutrition, Supplier<InteractionResult> interaction) {
		IntConsumer previous = this.alayacore$cakeNutrition;
		this.alayacore$cakeNutrition = nutrition;
		try {
			return interaction.get();
		} finally {
			this.alayacore$cakeNutrition = previous;
		}
	}

	@Inject(method = "eat(IF)V", at = @At("RETURN"))
	private void alayacore$cakeSlice(int nutrition, float saturation, CallbackInfo ci) {
		if (this.alayacore$cakeNutrition != null) {
			this.alayacore$cakeNutrition.accept(nutrition);
		}
	}
}
