package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The {@code [abilities]} section of {@code config/alayacore.toml} (plan 8).
 *
 * @param cooldownSeconds per ability, one cooldown per mode
 */
public record AbilityConfig(double holdThresholdSeconds, Map<Identifier, List<Double>> cooldownSeconds) {
	public static final AbilityConfig DEFAULTS = new AbilityConfig(0.25, Map.of());

	/** @param cooldownDefaults every registered ability with its built-in cooldowns, in registry order */
	public static AbilityConfig read(ConfigSection s, Map<Identifier, List<Double>> cooldownDefaults) {
		double threshold = s.number("hold_threshold_seconds", " A press released sooner is a tap; held longer, a hold.",
			DEFAULTS.holdThresholdSeconds, 0.0, Double.MAX_VALUE);
		ConfigSection cooldowns = s.section("cooldown_seconds", " Cooldown of every ability, one number per mode, starting when a use ends.");
		Map<Identifier, List<Double>> values = new LinkedHashMap<>();
		cooldownDefaults.forEach((id, defaults) -> values.put(id, cooldowns.numbers(id.toString(), null, defaults, 0.0, Double.MAX_VALUE)));
		return new AbilityConfig(threshold, Collections.unmodifiableMap(values));
	}

	public int holdThresholdTicks() {
		return AbilityRules.toTicks(this.holdThresholdSeconds);
	}

	/** This mode's cooldown; the ability's built-in one if the config was not read yet. */
	public double cooldownSeconds(Identifier id, Ability ability, int mode) {
		return this.cooldownSeconds.getOrDefault(id, ability.defaultCooldownSeconds()).get(mode);
	}
}
