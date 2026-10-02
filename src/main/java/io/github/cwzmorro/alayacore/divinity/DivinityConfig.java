package io.github.cwzmorro.alayacore.divinity;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * The {@code [divinity]} section of {@code config/alayacore.toml} (plan 10): divinity by entity type id, over
 * the data files' defaults. Empty by default.
 */
public record DivinityConfig(Map<Identifier, Double> byEntityType) {
	public static final DivinityConfig DEFAULTS = new DivinityConfig(Map.of());
	public static final String COMMENT = " Divinity (0-10) by entity type id, over the defaults in data files (data/<namespace>/alayacore/divinity/):\n"
		+ "   \"minecraft:warden\" = 2.0";

	public static DivinityConfig read(ConfigSection s) {
		return new DivinityConfig(s.numbersById(DEFAULTS.byEntityType, 0.0, DivinityRules.MAX));
	}

	public Optional<Double> divinity(Identifier entityType) {
		return Optional.ofNullable(this.byEntityType.get(entityType));
	}
}
