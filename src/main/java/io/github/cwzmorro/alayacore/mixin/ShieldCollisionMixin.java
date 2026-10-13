package io.github.cwzmorro.alayacore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.cwzmorro.alayacore.shield.SolidShields;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Vanilla's whole-entity collision box is too large for a tilted disc; use the shield's shape.
@Mixin(Entity.class)
abstract class ShieldCollisionMixin {
	@WrapOperation(method = "collide", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private List<VoxelShape> alayacore$solidShields(Level level, Entity mover, AABB area, Operation<List<VoxelShape>> original) {
		return SolidShields.withShields(mover, area, original.call(level, mover, area));
	}
}
