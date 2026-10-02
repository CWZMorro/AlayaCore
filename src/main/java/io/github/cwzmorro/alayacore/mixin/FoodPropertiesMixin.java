package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.mana.ManaEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mana after eating a food item: by its hunger points, plus its sweet bonus if it has one (plan 7). */
@Mixin(FoodProperties.class)
abstract class FoodPropertiesMixin {
	@Inject(method = "onConsume", at = @At("TAIL"))
	private void alayacore$manaFromFood(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
		if (level.isClientSide()) {
			return;
		}
		ManaEvents.onAte(user, ((FoodProperties) (Object) this).nutrition(), BuiltInRegistries.ITEM.getKey(stack.getItem()));
	}
}
