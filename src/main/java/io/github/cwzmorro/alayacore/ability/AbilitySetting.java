package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.util.Steps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A number the player chooses for an ability in the preset screen, from {@code min} to {@code max} in steps of
 * {@code step}. The label's key is {@code ability.<namespace>.<path>.setting.<id>}, with the value as its argument.
 *
 * @param id           the setting's name, unique within its ability
 * @param defaultValue what a player who never chose gets, moved to the nearest allowed value
 */
public record AbilitySetting(String id, double min, double max, double step, double defaultValue) {
	public AbilitySetting {
		if (id.isEmpty() || !(min < max) || !(step > 0.0)) {
			throw new IllegalArgumentException("bad ability setting " + id + ": " + min + ".." + max + " step " + step);
		}
		defaultValue = Steps.snap(min, max, step, defaultValue);
	}

	public double clamp(double value) {
		return Steps.snap(this.min, this.max, this.step, value);
	}

	public Component label(Identifier ability, double value) {
		return Component.translatable(ability.toLanguageKey("ability", "setting." + this.id), value);
	}
}
