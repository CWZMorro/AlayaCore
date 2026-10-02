package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.player;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.servant;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.client.hud.ServantHud;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.progression.ClassGrade;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;

/**
 * Shows the player HUD (plan 14) as a servant and as a human, with screenshots in the run folder's
 * {@code screenshots/}: portrait, class badge, class name, HP "current | max", mana percent, hidden
 * hearts and armor in the hearts' row; a player standing still keeps the same pixels in the portrait.
 */
public class ServantHudClientTest implements FabricClientGameTest {
	/** Max mana for sync 100% (plan 13.2): an Archer without its true name, 70% of the way to its real stats. */
	private static final double FULL_SYNC_MANA = 100_000;
	private static final double MANA_FRACTION = 0.75;
	private static final int TICKS_TO_SETTLE = 5;
	/** Longer than vanilla's hurt flash, so the portrait is not red in the screenshot. */
	private static final int TICKS_AFTER_DAMAGE = 20;
	/** A player standing still is checked to look the same in the portrait this many ticks apart. */
	private static final int STILL_TICKS = 20;
	/** A portrait pixel counts as changed past this much in a colour channel (0 to 255); less is the sky behind it. */
	private static final int STILL_TOLERANCE = 24;
	/**
	 * Window sizes and GUI scales the HUD is also checked at: 1 to 4 screen pixels per HUD texel, so every
	 * copy of the class badge is drawn once (GUI 2: 1, GUI 3 and 4: 2, GUI 6: 3, GUI 8: 4).
	 */
	private static final List<ScreenCheck> SCREEN_CHECKS = List.of(
		new ScreenCheck("1080p", 1920, 1080, new int[] {2, 3, 4}),
		new ScreenCheck("1440p", 2560, 1440, new int[] {6}),
		new ScreenCheck("2160p", 3840, 2160, new int[] {8}));

	private record ScreenCheck(String name, int width, int height, int[] guiScales) {
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		badgeSurvivesRejoining(context);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(ServantSelectionScreen.class);
			// Answered on the server directly; the screen only closes itself when clicked, so close it here.
			world.getServer().runOnServer(server -> ServantSelection.choose(player(server), servant(TestServants.ARCHER)));
			context.setScreen(() -> null);
			world.getClientLevel().waitForChunksRender();

			world.getServer().runOnServer(server -> AlayaMana.update(player(server), m -> m.withMax(FULL_SYNC_MANA)));
			world.getServer().runCommand("item replace entity @p armor.chest with minecraft:iron_chestplate");
			context.waitTicks(TICKS_TO_SETTLE);
			world.getServer().runCommand("damage @p 100 minecraft:generic");
			world.getServer().runOnServer(server -> AlayaMana.update(player(server), m -> m.withCurrent(m.max() * MANA_FRACTION)));
			context.waitTicks(TICKS_AFTER_DAMAGE);
			context.takeScreenshot("hud-01-archer");
			portraitHoldsStill(context, world);
			screenshotsAtScreenSizes(context);
			// The Classes tab with the Black, Bronze and Silver this sync has earned.
			context.setScreen(() -> {
				ClientAdvancements advancements = Minecraft.getInstance().getConnection().getAdvancements();
				advancements.setSelectedTab(advancements.get(ClassGrades.ROOT), false);
				return new AdvancementsScreen(advancements);
			});
			context.takeScreenshot("hud-01b-classes-tab");
			context.setScreen(() -> null);

			world.getServer().runOnServer(server -> {
				ServantSelection.open(player(server));
				ServantSelection.choose(player(server), human());
			});
			context.waitTicks(TICKS_TO_SETTLE);
			context.setScreen(() -> null);
			context.takeScreenshot("hud-02-human-player-name");

			world.getServer().runCommand("gamemode creative @p");
			context.waitTicks(TICKS_TO_SETTLE);
			context.takeScreenshot("hud-03-creative");
			world.getServer().runCommand("gamemode spectator @p");
			context.waitTicks(TICKS_TO_SETTLE);
			context.takeScreenshot("hud-04-spectator-portrait-only");
		}
	}

	/** The class badge the client shows is the player's grade, also after leaving the world and joining it again. */
	private static void badgeSurvivesRejoining(ClientGameTestContext context) {
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(ServantSelectionScreen.class);
			world.getServer().runOnServer(server -> ServantSelection.choose(player(server), servant(TestServants.ARCHER)));
			context.setScreen(() -> null);
			world.getServer().runOnServer(server -> AlayaMana.update(player(server), m -> m.withMax(FULL_SYNC_MANA)));
			context.waitTicks(TICKS_TO_SETTLE);
			checkClientBadge(context, "after earning Silver");
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getClientLevel().waitForChunksRender();
			context.waitTicks(TICKS_TO_SETTLE);
			checkClientBadge(context, "after joining again");
		}
	}

	private static void checkClientBadge(ClientGameTestContext context, String when) {
		ClassGrade shown = context.computeOnClient(client -> client.player.getAttachedOrElse(ClassGrades.GRADE, ClassGrade.BLACK));
		if (shown != ClassGrade.SILVER) {
			throw new AssertionError("The badge shows " + shown + " " + when + ", not SILVER");
		}
	}

	/**
	 * Standing still, the portrait shows the same pixels a while later. The sky shows through the portrait's
	 * half transparent background, so time stands still and the clouds are off meanwhile, and the sky's own
	 * slight brightening is let pass.
	 */
	private static void portraitHoldsStill(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("gamerule advance_time false");
		CloudStatus clouds = context.computeOnClient(client -> client.options.cloudStatus().get());
		context.runOnClient(client -> client.options.cloudStatus().set(CloudStatus.OFF));
		context.waitTicks(TICKS_TO_SETTLE);
		BufferedImage before = image(context.takeScreenshot("hud-01c-portrait-still"));
		context.waitTicks(STILL_TICKS);
		BufferedImage after = image(context.takeScreenshot("hud-01d-portrait-still-later"));
		context.runOnClient(client -> client.options.cloudStatus().set(clouds));
		world.getServer().runCommand("gamerule advance_time true");
		int[] box = context.computeOnClient(client -> ServantHud.portraitOnScreen(client.getWindow().getGuiScale()));
		int changed = 0;
		for (int y = box[1]; y < box[1] + box[3]; y++) {
			for (int x = box[0]; x < box[0] + box[2]; x++) {
				if (differs(before.getRGB(x, y), after.getRGB(x, y))) {
					changed++;
				}
			}
		}
		if (changed > 0) {
			throw new AssertionError(changed + " pixels of the portrait changed while the player stood still");
		}
	}

	private static boolean differs(int rgb, int other) {
		for (int shift = 0; shift <= 16; shift += 8) {
			if (Math.abs((rgb >> shift & 0xFF) - (other >> shift & 0xFF)) > STILL_TOLERANCE) {
				return true;
			}
		}
		return false;
	}

	private static BufferedImage image(Path screenshot) {
		try {
			return ImageIO.read(screenshot.toFile());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void screenshotsAtScreenSizes(ClientGameTestContext context) {
		int width = context.computeOnClient(client -> client.getWindow().getWidth());
		int height = context.computeOnClient(client -> client.getWindow().getHeight());
		int guiScale = context.computeOnClient(client -> client.options.guiScale().get());
		for (ScreenCheck check : SCREEN_CHECKS) {
			context.getInput().resizeWindow(check.width(), check.height());
			for (int scale : check.guiScales()) {
				setGuiScale(context, scale);
				context.takeScreenshot("hud-01-archer-" + check.name() + "-gui-" + scale);
			}
		}
		setGuiScale(context, guiScale);
		context.getInput().resizeWindow(width, height);
	}

	private static void setGuiScale(ClientGameTestContext context, int scale) {
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.waitTick();
	}
}
