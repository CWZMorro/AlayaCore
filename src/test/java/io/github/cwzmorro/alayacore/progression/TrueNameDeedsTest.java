package io.github.cwzmorro.alayacore.progression;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

// The deed kinds are a game registry, so reading a deed's condition is checked in ProgressionGameTests.
class TrueNameDeedsTest {
	@Test
	void aDeedNeedsExactlyOneCondition() {
		String json = """
			{"branch": "a:b", "awakening": "a:c", "deeds": [{"advancement": "a:d", "kill": "minecraft:warden", "win_raid": true}]}""";
		assertTrue(TrueNameDeeds.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).isError());
	}
}
