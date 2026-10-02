package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.divinity.Divinity;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/** Divinity inside a running server (plan 10). */
public class DivinityGameTests {
	private static final BlockPos INSIDE = new BlockPos(1, 1, 1);
	/** The test mod's data file {@code data/minecraft/alayacore/divinity/zombie.json}. */
	private static final double ZOMBIE_DIVINITY = 3.0;
	/** Between the 50% and 75% steps, clear of the boundaries. */
	private static final double MID_SYNC = 0.6;
	private static final int MID_SYNC_DIVINITY = 3;

	@GameTest
	public void entityTypesTakeTheirDataFileElseNone(GameTestHelper helper) {
		check(helper, "zombie (data file)", ZOMBIE_DIVINITY, Divinity.of(helper.spawn(EntityType.ZOMBIE, INSIDE)));
		check(helper, "pig (no data)", 0.0, Divinity.of(helper.spawn(EntityType.PIG, INSIDE)));
		helper.succeed();
	}

	@GameTest
	public void servantDivinityStepsUpWithSync(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.DIVINE);
		check(helper, "at the start", 1.0, Divinity.of(player));
		AlayaMana.update(player, m -> m.withMax(AlayaConfig.progression().manaForSync(MID_SYNC)));
		check(helper, "at 60% sync", MID_SYNC_DIVINITY, Divinity.of(player));
		AlayaMana.update(player, m -> m.withMax(AlayaConfig.progression().manaForSync(1.0)));
		check(helper, "at full sync", TestServants.DIVINE_MAX, Divinity.of(player));
		helper.succeed();
	}

	@GameTest
	public void humansAndServantsWithoutDivinityHaveNone(GameTestHelper helper) {
		ServerPlayer human = join(helper);
		ServantSelection.choose(human, human());
		check(helper, "human", 0.0, Divinity.of(human));
		check(helper, "servant without divinity", 0.0, Divinity.of(joinAs(helper, TestServants.SOLO)));
		helper.succeed();
	}
}
