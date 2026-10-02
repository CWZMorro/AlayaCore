package io.github.cwzmorro.alayacore.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets a shield make a projectile hit it, through the projectile's own hit handling (damage, explosion, pierce). */
@Mixin(Projectile.class)
public interface ProjectileInvoker {
	@Invoker("hitTargetOrDeflectSelf")
	ProjectileDeflection alayacore$hitTargetOrDeflectSelf(HitResult hit);
}
