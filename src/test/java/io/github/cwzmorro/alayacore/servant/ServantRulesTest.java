package io.github.cwzmorro.alayacore.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ServantRulesTest {
	private static final Identifier GILGAMESH = Identifier.fromNamespaceAndPath("test", "gilgamesh");
	private static final Identifier ARTORIA = Identifier.fromNamespaceAndPath("test", "artoria");
	private static final UUID ME = UUID.randomUUID();
	private static final UUID OTHER = UUID.randomUUID();

	@Test
	void takenByAnotherPlayerIsNotFree() {
		Map<UUID, Identifier> owners = Map.of(OTHER, GILGAMESH);
		assertFalse(ServantRules.isFree(GILGAMESH, owners, ME, false));
		assertTrue(ServantRules.isFree(ARTORIA, owners, ME, false));
	}

	@Test
	void ownServantCountsAsFree() {
		assertTrue(ServantRules.isFree(GILGAMESH, Map.of(ME, GILGAMESH), ME, false));
	}

	@Test
	void duplicatesAllowedMakesEverythingFree() {
		assertTrue(ServantRules.isFree(GILGAMESH, Map.of(OTHER, GILGAMESH), ME, true));
	}

	@Test
	void freeKeepsOrder() {
		assertEquals(List.of(ARTORIA), ServantRules.free(List.of(GILGAMESH, ARTORIA), Map.of(OTHER, GILGAMESH), ME, false));
		assertEquals(List.of(GILGAMESH, ARTORIA), ServantRules.free(List.of(GILGAMESH, ARTORIA), Map.of(), ME, false));
	}
}
