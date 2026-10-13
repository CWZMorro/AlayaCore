package io.github.cwzmorro.alayacore.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumbersTest {
	@Test
	void wholeValuesLoseTheirDecimalsAndOthersKeepThem() {
		assertEquals("5000", String.valueOf(Numbers.shown(5000.0)));
		assertEquals("2.5", String.valueOf(Numbers.shown(2.5)));
		assertEquals("3", String.valueOf(Numbers.shown(3.0F)));
		assertEquals("7", String.valueOf(Numbers.shown(7)));
		assertEquals("B", Numbers.shown("B"));
	}
}
