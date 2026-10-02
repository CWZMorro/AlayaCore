package io.github.cwzmorro.alayacore.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class AbilityPresetsTest {
	private static final Identifier GATES = Identifier.fromNamespaceAndPath("test", "gates");

	@Test
	void slotsAreSetModesWrapAndSlotsClear() {
		AbilityPresets presets = AbilityPresets.EMPTY.with(1, 2, Optional.of(GATES));
		assertEquals(Optional.of(new AbilityPresets.Slot(1, 2, GATES, 0)), presets.slot(1, 2));
		assertEquals(2, presets.withModeShifted(1, 2, -1, 3).slot(1, 2).orElseThrow().mode());
		assertEquals(0, presets.withModeShifted(1, 2, 3, 3).slot(1, 2).orElseThrow().mode());
		assertEquals(presets, presets.withModeShifted(0, 0, 1, 3), "an empty slot has no mode");
		assertTrue(presets.with(1, 2, Optional.empty()).slots().isEmpty());
	}

	@Test
	void activePresetWraps() {
		// plan 8: 5 presets
		assertEquals(4, AbilityPresets.EMPTY.withActive(-1).active());
		assertEquals(0, AbilityPresets.EMPTY.withActive(AbilityPresets.PRESETS).active());
		assertFalse(AbilityPresets.isValid(AbilityPresets.PRESETS, 0));
		assertFalse(AbilityPresets.isValid(0, AbilityPresets.SLOTS));
	}
}
