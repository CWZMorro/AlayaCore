package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// Filtering the block list preserves entity damage while protecting marble terrain.
@Mixin(ServerExplosion.class)
abstract class MarbleExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@ModifyReturnValue(method = "calculateExplodedPositions", at = @At("RETURN"))
	private List<BlockPos> alayacore$keepMarbleTerrain(List<BlockPos> blocks) {
		// Vanilla shuffles this list afterwards, so it is filtered in place and stays mutable.
		blocks.removeIf(pos -> RealityMarbles.protects(this.level, pos));
		return blocks;
	}
}
