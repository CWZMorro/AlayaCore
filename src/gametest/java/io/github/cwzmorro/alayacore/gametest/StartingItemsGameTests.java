package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.release;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.servant;

import io.github.cwzmorro.alayacore.servant.ServantSelection;
import io.github.cwzmorro.alayacore.servant.ServantStartingItems;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public class StartingItemsGameTests {
	@GameTest
	public void startingItemsAreGivenOnceAndNeverReplaced(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.STARTER);
		check(helper, "given on becoming the servant", true,
			player.getInventory().countItem(Items.STICK) == 1 && player.getInventory().countItem(Items.APPLE) == 1);
		player.getInventory().clearContent();
		ServantStartingItems.give(player);
		check(helper, "a lost item is never replaced", true, player.getInventory().isEmpty());

		release(helper.getLevel().getServer(), TestServants.STARTER);
		ServerPlayer marked = join(helper);
		ServantStartingItems.markGiven(marked, List.of(Items.STICK));
		ServantSelection.choose(marked, servant(TestServants.STARTER));
		check(helper, "an item already marked given is not given", true,
			marked.getInventory().countItem(Items.STICK) == 0 && marked.getInventory().countItem(Items.APPLE) == 1);

		ServerPlayer human = join(helper);
		ServantStartingItems.give(human);
		check(helper, "a human receives nothing", true, human.getInventory().isEmpty());
		helper.succeed();
	}
}
