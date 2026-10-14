package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.bow.ArcherBows;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BowItem.class)
abstract class ArcherBowMixin {
	@Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
	private float alayacore$drawPower(int time, ItemStack stack, Level level, LivingEntity entity, int remaining) {
		return ArcherBows.power(entity, time);
	}

	@ModifyVariable(method = "shootProjectile", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float alayacore$arrowSpeed(float power, LivingEntity shooter, Projectile projectile, int index, float originalPower,
		float uncertainty, float angle, @Nullable LivingEntity target) {
		return power * ArcherBows.speedMultiplier(ArcherBows.bar(shooter));
	}
}
