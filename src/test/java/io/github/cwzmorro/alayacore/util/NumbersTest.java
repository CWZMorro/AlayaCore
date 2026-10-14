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

	@Test
	void tenthsAlwaysHaveOneDecimal() {
		assertEquals("1.5", Numbers.tenths(1.5));
		assertEquals("20.0", Numbers.tenths(20.0));
		assertEquals("0.1", Numbers.tenths(0.06));
	}

	@Test
	void plainTextHasNoTrailingZerosOrExponent() {
		assertEquals("50", Numbers.plain(50.0));
		assertEquals("0.7", Numbers.plain(0.7));
		assertEquals("10000000", Numbers.plain(1.0E7));
	}
}
