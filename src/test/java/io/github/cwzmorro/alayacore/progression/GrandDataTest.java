package io.github.cwzmorro.alayacore.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** Plan 13.4: titles in qualifying order, never straight back to the one who lost it. */
class GrandDataTest {
	private static final Identifier SABER = Identifier.fromNamespaceAndPath("alayacore", "saber");
	private static final UUID FIRST = UUID.randomUUID();
	private static final UUID SECOND = UUID.randomUUID();
	private static final UUID THIRD = UUID.randomUUID();

	@Test
	void earliestQualifiedGetsTheTitle() {
		GrandData data = GrandData.EMPTY.qualify(SABER, FIRST, 1).qualify(SABER, SECOND, 1);
		assertTrue(data.isGrand(SABER, FIRST));
		assertFalse(data.isGrand(SABER, SECOND));
	}

	@Test
	void nextInLineTakesItAndTheLoserMustQualifyAgain() {
		GrandData data = GrandData.EMPTY.qualify(SABER, FIRST, 1).qualify(SABER, SECOND, 1).qualify(SABER, THIRD, 1)
			.loseTitle(SABER, FIRST, 1);
		assertTrue(data.isGrand(SABER, SECOND));
		assertFalse(data.isGrand(SABER, FIRST));
		data = data.loseTitle(SABER, SECOND, 1);
		assertTrue(data.isGrand(SABER, THIRD));
		assertFalse(data.isGrand(SABER, FIRST)); // not back without qualifying again
	}

	@Test
	void titleStaysEmptyWhenNobodyIsInLine() {
		GrandData data = GrandData.EMPTY.qualify(SABER, FIRST, 1).loseTitle(SABER, FIRST, 1);
		assertFalse(data.isGrand(SABER, FIRST));
		assertTrue(data.qualify(SABER, FIRST, 1).isGrand(SABER, FIRST));
	}

	@Test
	void severalGrandsPerClass() {
		GrandData data = GrandData.EMPTY.qualify(SABER, FIRST, 2).qualify(SABER, SECOND, 2).qualify(SABER, THIRD, 2);
		assertTrue(data.isGrand(SABER, FIRST));
		assertTrue(data.isGrand(SABER, SECOND));
		assertFalse(data.isGrand(SABER, THIRD));
	}
}
