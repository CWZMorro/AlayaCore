package io.github.cwzmorro.alayacore.servant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.List;
import net.minecraft.resources.ResourceKey;

/** A data-defined servant, including contentless hidden entries. */
public record ServantDefinition(Servant servant, boolean hidden) {
	public static final Codec<ServantDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
		ResourceKey.codec(AlayaRegistries.SERVANT_CLASS_KEY).fieldOf("class").forGetter(value -> value.servant.servantClass()),
		ResourceKey.<Ability>codec(AlayaRegistries.ABILITY_KEY).listOf().optionalFieldOf("abilities", List.of())
			.forGetter(value -> value.servant.abilities()),
		Codec.BOOL.fieldOf("hidden").forGetter(ServantDefinition::hidden)
	).apply(i, (servantClass, abilities, hidden) -> new ServantDefinition(new Servant(servantClass, abilities), hidden)));
}
