package io.github.cwzmorro.alayacore.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.util.Percent;

/** Extra percent of maximum mana restored by a sweet food, keyed by item id in data. */
public record SweetFood(double percent) {
	public static final Codec<SweetFood> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.doubleRange(0.0, Percent.WHOLE).fieldOf("percent").forGetter(SweetFood::percent)
	).apply(i, SweetFood::new));
}
