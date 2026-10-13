package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.progression.ClassGrades;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Descriptions must be ready before the advancements are sent to players.
@Mixin(ServerAdvancementManager.class)
abstract class AdvancementLoadMixin {
	@Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
		at = @At("TAIL"))
	private void alayacore$describeGrades(Map<Identifier, Advancement> preparations, ResourceManager manager, ProfilerFiller profiler,
		CallbackInfo ci) {
		ClassGrades.describe((ServerAdvancementManager) (Object) this);
	}
}
