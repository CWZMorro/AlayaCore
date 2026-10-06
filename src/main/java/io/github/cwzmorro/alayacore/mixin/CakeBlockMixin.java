package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.cwzmorro.alayacore.mana.ManaEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Cake is a block, so its slices never go through the food item path in FoodPropertiesMixin; candle cakes eat here too.
@Mixin(CakeBlock.class)
abstract class CakeBlockMixin {
	@WrapOperation(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
	private static void alayacore$manaFromCake(FoodData food, int nutrition, float saturation, Operation<Void> original,
		@Local(argsOnly = true) BlockState cake, @Local(argsOnly = true) Player player) {
		original.call(food, nutrition, saturation);
		if (!player.level().isClientSide()) {
			ManaEvents.onAte(player, nutrition, BuiltInRegistries.ITEM.getKey(cake.getBlock().asItem()));
		}
	}
}
