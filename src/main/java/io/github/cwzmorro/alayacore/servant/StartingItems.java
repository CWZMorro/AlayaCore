package io.github.cwzmorro.alayacore.servant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/** A servant's starting items, in the order they are given ({@link ServantStartingItems}). */
public record StartingItems(List<Item> items) {
	public static final Codec<StartingItems> CODEC = RecordCodecBuilder.create(i -> i.group(
		BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("items").forGetter(StartingItems::items)
	).apply(i, StartingItems::new));
}
