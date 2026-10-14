package io.github.cwzmorro.alayacore.servant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.util.Percent;

/**
 * The tunable values of one servant class, under {@code [classes."<id>"]} in the config.
 *
 * @param real                  the class's real values, reached at sync 100% + true name
 * @param manaRegenBonusPercent extra mana regeneration of all kinds, in percent (Archer: 20)
 * @param regenWhileCasting     mana keeps regenerating while casting or holding an ability (Archer)
 */
public record ServantClassConfig(ServantStats real, double manaRegenBonusPercent, boolean regenWhileCasting) {
	public static final MapCodec<ServantClassConfig> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		ServantStats.CODEC.fieldOf("real").forGetter(ServantClassConfig::real),
		Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("mana_regen_bonus_percent", 0.0).forGetter(ServantClassConfig::manaRegenBonusPercent),
		Codec.BOOL.optionalFieldOf("regen_while_casting", false).forGetter(ServantClassConfig::regenWhileCasting)
	).apply(i, ServantClassConfig::new));

	public static ServantClassConfig read(ConfigSection s, ServantClassConfig defaults) {
		return new ServantClassConfig(
			ServantStats.read(s, defaults.real),
			s.number("mana_regen_bonus_percent", null, defaults.manaRegenBonusPercent, 0.0, Double.MAX_VALUE),
			s.bool("regen_while_casting", null, defaults.regenWhileCasting)
		);
	}

	public double manaRegenMultiplier() {
		return 1.0 + Percent.toFraction(this.manaRegenBonusPercent);
	}
}
