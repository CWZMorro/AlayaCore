package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * What any weapon ability may destroy (never what a reality marble protects or what cannot be broken), and destroying it without
 * drops. Whether it may at all is {@link #allowed}.
 */
public final class WeaponBlocks {
	private WeaponBlocks() {
	}

	public static boolean allowed(ServerLevel level) {
		return level.getGameRules().get(AlayaGameRules.WEAPON_BLOCK_DAMAGE);
	}

	public static boolean breakable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !RealityMarbles.protects(level, pos) && !state.isAir() && state.getDestroySpeed(level, pos) >= 0;
	}

	public static void breakWithoutDrops(ServerLevel level, BlockPos pos) {
		if (breakable(level, pos)) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/**
	 * Boils the water out of a breakable block without drops: a waterlogged block is drained, water itself and water plants
	 * go to air. True if water was removed.
	 */
	public static boolean vaporize(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.getFluidState().is(FluidTags.WATER) || !breakable(level, pos)) {
			return false;
		}
		level.setBlock(pos, state.hasProperty(BlockStateProperties.WATERLOGGED)
			? state.setValue(BlockStateProperties.WATERLOGGED, false) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		return true;
	}
}
