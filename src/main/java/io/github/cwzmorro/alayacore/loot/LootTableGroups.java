package io.github.cwzmorro.alayacore.loot;

import io.github.cwzmorro.alayacore.AlayaCore;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

/** Additive loot-table lists, extensible by content mods and datapacks without code. */
public final class LootTableGroups {
	public static final ResourceKey<Registry<LootTableGroup>> KEY = ResourceKey.createRegistryKey(AlayaCore.id("loot_table_group"));

	private LootTableGroups() {
	}

	public static void register() {
		DynamicRegistries.register(KEY, LootTableGroup.CODEC);
	}

	/** Returns all tables contributed to the group; intended for resource reload rather than individual loot rolls. */
	public static Set<ResourceKey<LootTable>> tables(HolderLookup.Provider registries, Identifier group) {
		return registries.lookup(KEY).map(registry -> registry.listElements()
			.map(holder -> holder.value()).filter(entry -> entry.group().equals(group)).flatMap(entry -> entry.tables().stream())
			.collect(Collectors.toUnmodifiableSet())).orElse(Set.of());
	}
}
