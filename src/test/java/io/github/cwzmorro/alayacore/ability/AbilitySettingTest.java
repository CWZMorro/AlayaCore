package io.github.cwzmorro.alayacore.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class AbilitySettingTest {
	private static final Identifier ABILITY = Identifier.fromNamespaceAndPath("test", "shield");
	private static final AbilitySetting DISTANCE = new AbilitySetting("distance", 2.0, 8.0, 0.5, 2.5);

	@Test
	void valuesSnapToStepsInsideTheRange() {
		assertEquals(2.0, DISTANCE.clamp(-3.0));
		assertEquals(8.0, DISTANCE.clamp(100.0));
		assertEquals(3.5, DISTANCE.clamp(3.6));
		assertEquals(2.0, DISTANCE.clamp(Double.NaN));
		assertEquals(5.5, new AbilitySetting("size", 5.0, 8.0, 0.5, 5.6).defaultValue(), "a default off the steps moves to the nearest");
		assertThrows(IllegalArgumentException.class, () -> new AbilitySetting("empty", 3.0, 3.0, 0.5, 3.0));
	}

	@Test
	void choicesDefaultSaveAndSend() {
		AbilitySettingValues chosen = AbilitySettingValues.EMPTY;
		assertEquals(2.5, chosen.of(ABILITY, DISTANCE), "never chosen reads as the default");
		chosen = chosen.with(ABILITY, DISTANCE.id(), 7.0);
		assertEquals(7.0, chosen.of(ABILITY, DISTANCE));

		assertEquals(chosen, AbilitySettingValues.CODEC.parse(JsonOps.INSTANCE, AbilitySettingValues.CODEC.encodeStart(JsonOps.INSTANCE, chosen)
			.getOrThrow()).getOrThrow(), "saved");
		ByteBuf buffer = Unpooled.buffer();
		AbilitySettingValues.STREAM_CODEC.encode(buffer, chosen);
		assertEquals(chosen, AbilitySettingValues.STREAM_CODEC.decode(buffer), "sent to the owner");
	}
}
