package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.cwzmorro.alayacore.mana.CakeFoods;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class CakeInteractionMixin {
	@WrapMethod(method = "useWithoutItem")
	private InteractionResult alayacore$cakeWithoutItem(Level level, Player player, BlockHitResult hit, Operation<InteractionResult> original) {
		return CakeFoods.interact((BlockState) (Object) this, player, () -> original.call(level, player, hit));
	}

	@WrapMethod(method = "useItemOn")
	private InteractionResult alayacore$cakeWithItem(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit,
		Operation<InteractionResult> original) {
		return CakeFoods.interact((BlockState) (Object) this, player, () -> original.call(stack, level, player, hand, hit));
	}
}
