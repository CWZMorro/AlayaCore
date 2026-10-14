package io.github.cwzmorro.alayacore.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class ServantClassCodecTest {
	private static final String ARCHER = """
		{"order": 1, "real": {"max_hp": 275, "attack": 24, "armor": 2, "toughness": 0, "speed_percent": 25, "knockback_resistance": 0.4},
		"mana_regen_bonus_percent": 20, "regen_while_casting": true}""";

	@Test
	void readsAClassFromData() {
		ServantClass servantClass = ServantClass.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(ARCHER)).getOrThrow();
		assertEquals(new ServantClass(1, new ServantClassConfig(new ServantStats(275, 24, 2, 0, 25, 0.4), 20.0, true)), servantClass);
	}

	@Test
	void optionalKeysDefaultToNoBonus() {
		String json = "{\"real\": {\"max_hp\": 300, \"attack\": 28, \"armor\": 2, \"toughness\": 1, \"speed_percent\": 20, \"knockback_resistance\": 0.5}}";
		ServantClass servantClass = ServantClass.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
		assertEquals(0, servantClass.order());
		assertEquals(new ServantClassConfig(new ServantStats(300, 28, 2, 1, 20, 0.5), 0.0, false), servantClass.defaults());
	}

	@Test
	void valuesBeyondTheGameLimitsAreRefused() {
		String json = "{\"real\": {\"max_hp\": 5000, \"attack\": 28, \"armor\": 2, \"toughness\": 1, \"speed_percent\": 20, \"knockback_resistance\": 0.5}}";
		assertTrue(ServantClass.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).isError());
	}
}
