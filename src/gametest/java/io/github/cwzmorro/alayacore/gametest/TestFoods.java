package io.github.cwzmorro.alayacore.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class TestFoods {
	private static final int NUTRITION = 2;
	public static final Block CAKE = block("test_cake");
	public static final Block UNTAGGED = block("untagged_food_block");

	private TestFoods() {
	}

	private static Block block(String path) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, TestServants.id(path));
		return Registry.register(BuiltInRegistries.BLOCK, key, new Block(BlockBehaviour.Properties.of().setId(key)) {
			@Override
			protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
				if (!player.canEat(false)) {
					return InteractionResult.PASS;
				}
				player.getFoodData().eat(NUTRITION, 0.1F);
				return InteractionResult.CONSUME;
			}
		});
	}

	public static void register() {
	}
}
