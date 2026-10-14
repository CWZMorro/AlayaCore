package io.github.cwzmorro.alayacore.divinity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A servant's initial and full-sync divinity, from
 * {@code data/<namespace>/alayacore/servant_divinity/<servant path>.json}. Missing data means no divinity.
 */
public record ServantDivinity(int start, int max) {
	public static final Codec<ServantDivinity> CODEC = RecordCodecBuilder.<ServantDivinity>create(i -> i.group(
		Codec.intRange(0, DivinityRules.MAX).fieldOf("start").forGetter(ServantDivinity::start),
		Codec.intRange(0, DivinityRules.MAX).fieldOf("max").forGetter(ServantDivinity::max)
	).apply(i, ServantDivinity::new)).validate(value -> value.start <= value.max
		? com.mojang.serialization.DataResult.success(value)
		: com.mojang.serialization.DataResult.error(() -> "start divinity exceeds max"));
}
