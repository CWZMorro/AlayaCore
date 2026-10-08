package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.ClientTests.FIRST_ROW_X;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.FIRST_ROW_Y;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.TAB_X;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.TAB_Y;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.clickPanel;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.moveToPanel;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.ROUND_TRIP_TICKS;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.player;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.servant;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.ability.AbilitySettings;
import io.github.cwzmorro.alayacore.client.screen.PresetScreen;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public class AbilitySettingsClientTest implements FabricClientGameTest {
	private static final int[] GUI_SCALES = {2, 3};
	private static final int LAST_ROW_Y = 105;
	private static final int ABILITY_SLIDER = 1;
	private static final double SLIDER_END = 1.0;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(ServantSelectionScreen.class);
			world.getServer().runOnServer(server -> ServantSelection.choose(player(server), servant(TestServants.CASTER)));
			context.setScreen(() -> null);
			world.getClientLevel().waitForChunksRender();
			context.getInput().resizeWindow(ClientTests.WIDE, ClientTests.TALL);
			context.runOnClient(client -> client.getToastManager().clear());

			context.getInput().pressKey(InputConstants.KEY_B);
			context.waitForScreen(PresetScreen.class);
			for (int scale : GUI_SCALES) {
				ClientTests.setGuiScale(context, scale);
				moveToPanel(context, FIRST_ROW_X, LAST_ROW_Y);
				context.getInput().scroll(-1);
				clickPanel(context, FIRST_ROW_X, LAST_ROW_Y);
				context.takeScreenshot("settings-scale-" + scale + "-closed");
				clickPanel(context, TAB_X, TAB_Y);
				context.waitTicks(ClientTests.TICKS_TO_SETTLE);
				context.takeScreenshot("settings-scale-" + scale + "-open");
				ClientTests.clickSlider(context, ABILITY_SLIDER, SLIDER_END);
				context.waitTicks(ROUND_TRIP_TICKS);
				double chosen = world.getServer().computeOnServer(server -> AbilitySettings.value(player(server),
					TestAbilities.CONFIGURABLE.identifier(), TestAbilities.REACH));
				check(chosen == TestAbilities.REACH.max(), "the slider's right end chose " + chosen);
				context.takeScreenshot("settings-scale-" + scale + "-chosen");
				ClientTests.closePresetSettings(context);
				clickPanel(context, FIRST_ROW_X, FIRST_ROW_Y);
			}
			context.setScreen(() -> null);
		}
	}
}
