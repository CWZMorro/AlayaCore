package io.github.cwzmorro.alayacore.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** An additive contribution to a named group of loot tables, for data-driven loot hooks. */
public record LootTableGroup(Identifier group, List<ResourceKey<LootTable>> tables) {
	public static final Codec<LootTableGroup> CODEC = RecordCodecBuilder.create(i -> i.group(
		Identifier.CODEC.fieldOf("group").forGetter(LootTableGroup::group),
		ResourceKey.codec(Registries.LOOT_TABLE).listOf().fieldOf("tables").forGetter(LootTableGroup::tables)
	).apply(i, LootTableGroup::new));

	public LootTableGroup {
		tables = List.copyOf(tables);
	}
}
