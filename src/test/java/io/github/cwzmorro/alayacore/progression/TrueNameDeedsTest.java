package io.github.cwzmorro.alayacore.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class TrueNameDeedsTest {
	@Test
	void readsTheDataFormat() {
		String json = """
			{"branch": "fatemod:gilgamesh/root",
			 "deeds": [{"advancement": "fatemod:gilgamesh/humbaba", "kill": "minecraft:warden"},
			           {"advancement": "fatemod:gilgamesh/bull_of_heaven", "win_raid": true}],
			 "awakening": "fatemod:gilgamesh/true_name"}""";
		TrueNameDeeds deeds = TrueNameDeeds.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
		assertEquals(2, deeds.deeds().size());
		assertEquals(Optional.of(Identifier.withDefaultNamespace("warden")), deeds.deeds().getFirst().kill());
		assertTrue(deeds.deeds().get(1).winRaid());
	}

	@Test
	void aDeedNeedsExactlyOneCondition() {
		String json = """
			{"branch": "a:b", "awakening": "a:c", "deeds": [{"advancement": "a:d", "kill": "minecraft:warden", "win_raid": true}]}""";
		assertTrue(TrueNameDeeds.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).isError());
	}
}
