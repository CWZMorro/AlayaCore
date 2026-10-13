package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;

import io.github.cwzmorro.alayacore.item.LostToVoid;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class LostToVoidGameTests {
	private static final double BELOW_THE_WORLD = 100.0;
	private static final List<ItemStack> LOST = new ArrayList<>();

	static {
		LostToVoid.register(Items.HEART_OF_THE_SEA, (level, item) -> LOST.add(item.getItem().copy()));
	}

	@GameTest
	public void onlyTheRegisteredItemsHandlerRuns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemEntity lost = new ItemEntity(level, 0, level.getMinY() - BELOW_THE_WORLD, 0, new ItemStack(Items.HEART_OF_THE_SEA));
		ItemEntity other = new ItemEntity(level, 0, level.getMinY() - BELOW_THE_WORLD, 0, new ItemStack(Items.NAUTILUS_SHELL));
		int before = LOST.size();
		lost.tick();
		other.tick();
		check(helper, "both fell out of the world", true, lost.isRemoved() && other.isRemoved());
		check(helper, "only the registered item's handler ran, once", true, LOST.size() == before + 1 && LOST.getLast().is(Items.HEART_OF_THE_SEA));
		helper.succeed();
	}
}
