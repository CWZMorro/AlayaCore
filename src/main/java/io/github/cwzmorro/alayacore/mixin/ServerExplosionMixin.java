package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.cwzmorro.alayacore.shield.Shields;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Nothing behind a shield is hurt, pushed or broken by an explosion on its far side (plan 9); Fabric has no event
 * for explosions.
 */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Shadow
	@Final
	private Vec3 center;

	@WrapOperation(
		method = "hurtEntities",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;ignoreExplosion(Lnet/minecraft/world/level/Explosion;)Z")
	)
	private boolean alayacore$skipShelteredEntities(Entity entity, Explosion explosion, Operation<Boolean> original) {
		return original.call(entity, explosion) || Shields.shelters(this.level, this.center, entity);
	}

	@ModifyReturnValue(method = "calculateExplodedPositions", at = @At("RETURN"))
	private List<BlockPos> alayacore$keepShieldedBlocks(List<BlockPos> blocks) {
		return Shields.unshielded(this.level, this.center, blocks);
	}
}
