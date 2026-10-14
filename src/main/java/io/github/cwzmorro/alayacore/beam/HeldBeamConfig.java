package io.github.cwzmorro.alayacore.beam;

import io.github.cwzmorro.alayacore.config.ConfigSection;

/**
 * The {@code [held_beams]} section of {@code config/alayacore.toml}: the bow-like movement shared by every held beam that opts into
 * it ({@link HeldBeam#startMovement}). Its rank's numbers are in {@code [ranks]}.
 */
public record HeldBeamConfig(double heldBeamMovementMultiplier) {
	public static final HeldBeamConfig DEFAULTS = new HeldBeamConfig(0.2);
	public static final String HELD_BEAM_MOVEMENT_MULTIPLIER = "held_beam_movement_multiplier";

	public static HeldBeamConfig read(ConfigSection s) {
		return new HeldBeamConfig(s.number(HELD_BEAM_MOVEMENT_MULTIPLIER,
			" Movement input multiplier while a held beam that slows its caster charges or fires; jumping remains allowed.",
			DEFAULTS.heldBeamMovementMultiplier, 0.0, 1.0));
	}
}
