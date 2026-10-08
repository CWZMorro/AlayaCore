package io.github.cwzmorro.alayacore.ability;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * What a player chose for the settings of their abilities. Immutable; a setting never
 * chosen is not stored and reads as its default.
 *
 * @param values per ability, per setting id, the chosen value
 */
public record AbilitySettingValues(Map<Identifier, Map<String, Double>> values) {
	public static final AbilitySettingValues EMPTY = new AbilitySettingValues(Map.of());
	public static final Codec<AbilitySettingValues> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.unboundedMap(Codec.STRING, Codec.DOUBLE))
		.xmap(AbilitySettingValues::new, AbilitySettingValues::values);
	public static final StreamCodec<ByteBuf, AbilitySettingValues> STREAM_CODEC = ByteBufCodecs.<ByteBuf, Identifier,
			Map<String, Double>, Map<Identifier, Map<String, Double>>>map(HashMap::new, Identifier.STREAM_CODEC,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE))
		.map(AbilitySettingValues::new, AbilitySettingValues::values);

	public AbilitySettingValues {
		Map<Identifier, Map<String, Double>> copy = new HashMap<>();
		values.forEach((ability, chosen) -> copy.put(ability, Map.copyOf(chosen)));
		values = Map.copyOf(copy);
	}

	public double of(Identifier ability, AbilitySetting setting) {
		return this.values.getOrDefault(ability, Map.of()).getOrDefault(setting.id(), setting.defaultValue());
	}

	public AbilitySettingValues with(Identifier ability, String setting, double value) {
		Map<Identifier, Map<String, Double>> values = new HashMap<>(this.values);
		Map<String, Double> chosen = new HashMap<>(values.getOrDefault(ability, Map.of()));
		chosen.put(setting, value);
		values.put(ability, chosen);
		return new AbilitySettingValues(values);
	}
}
