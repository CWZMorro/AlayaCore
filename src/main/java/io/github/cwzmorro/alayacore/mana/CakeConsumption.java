package io.github.cwzmorro.alayacore.mana;

import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.world.InteractionResult;

/** Scopes actual nutrition consumed during a tagged block interaction, without storing gameplay state. */
public interface CakeConsumption {
	InteractionResult alayacore$withCake(IntConsumer nutrition, Supplier<InteractionResult> interaction);
}
