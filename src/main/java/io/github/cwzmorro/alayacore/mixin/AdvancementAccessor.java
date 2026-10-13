package io.github.cwzmorro.alayacore.mixin;

import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the mod rebuild an advancement's chat name (its hover shows the description) after writing its description. */
@Mixin(Advancement.class)
public interface AdvancementAccessor {
	@Mutable
	@Accessor("name")
	void alayacore$setName(Optional<Component> name);

	@Invoker("decorateName")
	static Component alayacore$decorateName(DisplayInfo display) {
		throw new AssertionError();
	}
}
