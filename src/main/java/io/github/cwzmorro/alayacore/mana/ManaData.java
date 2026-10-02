package io.github.cwzmorro.alayacore.mana;

/**
 * An entity's mana: what it has now and its maximum. Immutable; change it through {@link ManaRules}.
 * Values are never rounded.
 */
public record ManaData(double current, double max) {
	public ManaData withCurrent(double current) {
		return new ManaData(current, this.max);
	}

	public ManaData withMax(double max) {
		return new ManaData(this.current, max);
	}
}
