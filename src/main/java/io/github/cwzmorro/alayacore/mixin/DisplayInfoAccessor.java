package io.github.cwzmorro.alayacore.mixin;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the mod write a loaded advancement's description from the live config (class grades' sync). */
@Mixin(DisplayInfo.class)
public interface DisplayInfoAccessor {
	@Mutable
	@Accessor("description")
	void alayacore$setDescription(Component description);
}
