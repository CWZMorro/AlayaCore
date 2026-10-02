package io.github.cwzmorro.alayacore.divinity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * An entity type's divinity (plan 10), from {@code data/<namespace>/alayacore/divinity/<entity type path>.json}:
 * the file's id is the entity type's id, so a content mod ships its own and a datapack can replace any of them.
 * The {@code [divinity]} config table overrides these.
 */
public record DivinityDefault(double divinity) {
	public static final Codec<DivinityDefault> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.doubleRange(0.0, DivinityRules.MAX).fieldOf("divinity").forGetter(DivinityDefault::divinity)
	).apply(i, DivinityDefault::new));
}
