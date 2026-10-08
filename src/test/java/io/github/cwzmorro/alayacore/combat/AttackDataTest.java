package io.github.cwzmorro.alayacore.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.cwzmorro.alayacore.rank.Rank;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttackDataTest {
	private static final UUID OWNER = UUID.randomUUID();
	private static final Rank RANK = Rank.parse("A");

	@Test
	void noblePhantasmsDoNotNeedToClash() {
		AttackData burst = new AttackData(OWNER, RANK, false, true);
		assertTrue(burst.noblePhantasm());
		assertFalse(burst.canClash());
		assertFalse(new AttackData(OWNER, RANK, true, false).noblePhantasm());
	}

	@Test
	void noblePhantasmFlagIsSaved() {
		AttackData beam = new AttackData(OWNER, RANK, true, true);
		JsonElement encoded = AttackData.CODEC.encodeStart(JsonOps.INSTANCE, beam).getOrThrow();
		assertEquals(beam, AttackData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
	}

	@Test
	void oldSavedAttacksDefaultToNotANoblePhantasm() {
		AttackData ordinary = new AttackData(OWNER, RANK, false, false);
		JsonObject encoded = AttackData.CODEC.encodeStart(JsonOps.INSTANCE, ordinary).getOrThrow().getAsJsonObject();
		encoded.remove("noble_phantasm");
		assertEquals(ordinary, AttackData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
	}
}
