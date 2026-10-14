package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;

import io.github.cwzmorro.alayacore.loot.LootTableGroups;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class LootTableGroupGameTests {
	// Contributed to by the test mod's loot_table_group/first.json and second.json.
	private static final Identifier GROUP = TestServants.id("test_group");
	private static final ResourceKey<LootTable> FIRST = table("chests/first");
	private static final ResourceKey<LootTable> SECOND = table("chests/second");
	private static final ResourceKey<LootTable> OTHER = table("chests/other");

	private static ResourceKey<LootTable> table(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, TestServants.id(path));
	}

	@GameTest
	public void contributionsToOneGroupAddUpAndAGroupKnowsOnlyItsOwn(GameTestHelper helper) {
		RegistryAccess registries = helper.getLevel().registryAccess();
		check(helper, "both files contribute", Set.of(FIRST, SECOND), LootTableGroups.tables(registries, GROUP));
		check(helper, "another group's table stays out", false, LootTableGroups.tables(registries, GROUP).contains(OTHER));
		check(helper, "an unknown group is empty", Set.of(), LootTableGroups.tables(registries, TestServants.id("no_such_group")));
		helper.succeed();
	}
}
