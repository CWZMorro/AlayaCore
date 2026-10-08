package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.ClientTests.FIRST_ROW_X;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.FIRST_ROW_Y;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.FIRST_SLOT_X;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.FIRST_SLOT_Y;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.TAB_X;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.TAB_Y;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.clickPanel;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.player;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.servant;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.client.screen.PresetScreen;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.config.ClientConfig;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

public class PresetScreenClientTest implements FabricClientGameTest {
	private static final int[] GUI_SCALES = {2, 3, 4};
	private static final double MAX_MANA = 20_000;
	private static final double CURRENT_MANA = 12_345;
	private static final int OPACITY_SLIDER = 0;
	private static final double LOW_OPACITY = 0.2;
	private static final double OPACITY_TOLERANCE = 0.05;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(ServantSelectionScreen.class);
			world.getServer().runOnServer(server -> {
				ServantSelection.choose(player(server), servant(TestServants.CASTER));
				AlayaMana.update(player(server), mana -> mana.withMax(MAX_MANA).withCurrent(CURRENT_MANA));
			});
			context.setScreen(() -> null);
			world.getClientLevel().waitForChunksRender();
			context.getInput().resizeWindow(ClientTests.WIDE, ClientTests.TALL);
			context.runOnClient(client -> client.getToastManager().clear());

			context.getInput().pressKey(InputConstants.KEY_B);
			context.waitForScreen(PresetScreen.class);
			clickPanel(context, FIRST_ROW_X, FIRST_ROW_Y);
			clickPanel(context, FIRST_SLOT_X, FIRST_SLOT_Y);
			for (int scale : GUI_SCALES) {
				ClientTests.setGuiScale(context, scale);
				context.takeScreenshot("preset-scale-" + scale + "-description");
				clickPanel(context, TAB_X, TAB_Y);
				context.waitTicks(ClientTests.TICKS_TO_SETTLE);
				context.takeScreenshot("preset-scale-" + scale + "-settings");
				ClientTests.clickSlider(context, OPACITY_SLIDER, LOW_OPACITY);
				double opacity = context.computeOnClient(client -> (double) ClientConfig.presetHudOpacity());
				check(Math.abs(opacity - LOW_OPACITY) < OPACITY_TOLERANCE, "the slider's fifth chose " + opacity);
				context.takeScreenshot("preset-scale-" + scale + "-opacity");
				ClientTests.closePresetSettings(context);
			}

			context.getInput().pressKey(InputConstants.KEY_B);
			context.waitTick();
			Path file = FabricLoader.getInstance().getConfigDir().resolve(ClientConfig.FILE_NAME);
			try {
				check(Files.readString(file).contains("opacity_percent"), ClientConfig.FILE_NAME + " does not hold the chosen opacity");
				ClientConfig.setPresetHudOpacity(1.0F);
				check(ClientConfig.load().isEmpty(), "client config failed to reload");
				check(Math.abs(ClientConfig.presetHudOpacity() - LOW_OPACITY) < OPACITY_TOLERANCE, "client config did not retain opacity");
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			context.takeScreenshot("preset-hud-low-opacity");
		}
	}

}
