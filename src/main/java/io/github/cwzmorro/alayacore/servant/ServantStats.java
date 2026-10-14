package io.github.cwzmorro.alayacore.servant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The stats a servant's progression moves between. Attack, armor, toughness and
 * knockback resistance add to the player's own and their gear; speed is a percent over walking speed.
 */
public record ServantStats(double maxHp, double attack, double armor, double toughness, double speedPercent, double knockbackResistance) {
	// Vanilla's attribute limits for the bonuses (armor 30, toughness 20, knockback resistance 1, max health 1024).
	private static final double MAX_ARMOR = 30.0;
	private static final double MAX_TOUGHNESS = 20.0;
	private static final double MAX_KNOCKBACK_RESISTANCE = 1.0;
	private static final double MAX_HEALTH = 1024.0;

	public static final Codec<ServantStats> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.doubleRange(1.0, MAX_HEALTH).fieldOf("max_hp").forGetter(ServantStats::maxHp),
		Codec.doubleRange(0.0, Double.MAX_VALUE).fieldOf("attack").forGetter(ServantStats::attack),
		Codec.doubleRange(0.0, MAX_ARMOR).fieldOf("armor").forGetter(ServantStats::armor),
		Codec.doubleRange(0.0, MAX_TOUGHNESS).fieldOf("toughness").forGetter(ServantStats::toughness),
		Codec.doubleRange(0.0, Double.MAX_VALUE).fieldOf("speed_percent").forGetter(ServantStats::speedPercent),
		Codec.doubleRange(0.0, MAX_KNOCKBACK_RESISTANCE).fieldOf("knockback_resistance").forGetter(ServantStats::knockbackResistance)
	).apply(i, ServantStats::new));

	public static final StreamCodec<ByteBuf, ServantStats> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.DOUBLE, ServantStats::maxHp,
		ByteBufCodecs.DOUBLE, ServantStats::attack,
		ByteBufCodecs.DOUBLE, ServantStats::armor,
		ByteBufCodecs.DOUBLE, ServantStats::toughness,
		ByteBufCodecs.DOUBLE, ServantStats::speedPercent,
		ByteBufCodecs.DOUBLE, ServantStats::knockbackResistance,
		ServantStats::new
	);

	public static ServantStats read(ConfigSection s, ServantStats defaults) {
		return new ServantStats(
			s.number("max_hp", null, defaults.maxHp, 1.0, MAX_HEALTH),
			s.number("attack", null, defaults.attack, 0.0, Double.MAX_VALUE),
			s.number("armor", null, defaults.armor, 0.0, MAX_ARMOR),
			s.number("toughness", null, defaults.toughness, 0.0, MAX_TOUGHNESS),
			s.number("speed_percent", null, defaults.speedPercent, 0.0, Double.MAX_VALUE),
			s.number("knockback_resistance", null, defaults.knockbackResistance, 0.0, MAX_KNOCKBACK_RESISTANCE)
		);
	}

	public static ServantStats between(ServantStats demi, ServantStats real, double fill) {
		return new ServantStats(
			lerp(demi.maxHp, real.maxHp, fill),
			lerp(demi.attack, real.attack, fill),
			lerp(demi.armor, real.armor, fill),
			lerp(demi.toughness, real.toughness, fill),
			lerp(demi.speedPercent, real.speedPercent, fill),
			lerp(demi.knockbackResistance, real.knockbackResistance, fill)
		);
	}

	private static double lerp(double from, double to, double fill) {
		return from + (to - from) * fill;
	}
}
