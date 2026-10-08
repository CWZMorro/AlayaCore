package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.ClientTests.TICKS_TO_SETTLE;
import static io.github.cwzmorro.alayacore.gametest.ClientTests.setGuiScale;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.player;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.servant;

import io.github.cwzmorro.alayacore.AlayaItems;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ServantHudClientTest implements FabricClientGameTest {
	private static final double FULL_SYNC_MANA = 100_000;
	private static final double MANA_FRACTION = 0.75;
	private static final int PROMOTIONS_TO_GOLD = 3;
	// Longer than vanilla's hurt flash, so the portrait is not red in the screenshot.
	private static final int TICKS_AFTER_DAMAGE = 20;
	private static final int STILL_TICKS = 20;
	// A portrait pixel counts as changed past this much in a colour channel (0 to 255); less is the sky behind it.
	private static final int STILL_TOLERANCE = 24;
	// Exercise each copy in the badge texture strip.
	private static final List<ScreenCheck> SCREEN_CHECKS = List.of(
		new ScreenCheck("1080p", 1920, 1080, new int[] {2, 3, 4}),
		new ScreenCheck("1440p", 2560, 1440, new int[] {6}),
		new ScreenCheck("2160p", 3840, 2160, new int[] {8}));

	private record ScreenCheck(String name, int width, int height, int[] guiScales) {
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		badgeSurvivesRejoining(context);
		badgeFollowsPromotion(context);
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

	private static void badgeSurvivesRejoining(ClientGameTestContext context) {
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(ServantSelectionScreen.class);
			world.getServer().runOnServer(server -> ServantSelection.choose(player(server), servant(TestServants.ARCHER)));
			context.setScreen(() -> null);
			world.getServer().runOnServer(server -> AlayaMana.update(player(server), m -> m.withMax(FULL_SYNC_MANA)));
			context.waitTicks(TICKS_TO_SETTLE);
			checkClientBadge(context, ClassGrade.SILVER, "after earning Silver");
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getClientLevel().waitForChunksRender();
			context.waitTicks(TICKS_TO_SETTLE);
			checkClientBadge(context, ClassGrade.SILVER, "after joining again");
		}
	}

	private static void badgeFollowsPromotion(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(ServantSelectionScreen.class);
			world.getServer().runOnServer(server -> ServantSelection.choose(player(server), servant(TestServants.ARCHER)));
			context.setScreen(() -> null);
			world.getClientLevel().waitForChunksRender();
			world.getServer().runCommand("gamemode creative @p");
			context.waitTicks(TICKS_TO_SETTLE);
			checkClientBadge(context, ClassGrade.BLACK, "before the promotion");
			context.takeScreenshot("hud-05-badge-before-promotion");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlayaItems.GRADE_PROMOTION));
				for (int step = 0; step < PROMOTIONS_TO_GOLD; step++) {
					player.getMainHandItem().use(player.level(), player, InteractionHand.MAIN_HAND);
				}
			});
			context.waitTicks(TICKS_TO_SETTLE);
			checkClientBadge(context, ClassGrade.GOLD, "after the promotions");
			context.takeScreenshot("hud-06-badge-after-promotion");
		}
	}

	private static void checkClientBadge(ClientGameTestContext context, ClassGrade expected, String when) {
		ClassGrade shown = context.computeOnClient(client -> client.player.getAttachedOrElse(ClassGrades.GRADE, ClassGrade.BLACK));
		if (shown != expected) {
			throw new AssertionError("The badge shows " + shown + " " + when + ", not " + expected);
		}
	}

	// The sky shows through the portrait's half transparent background, so time stands still and the clouds are off meanwhile.
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

}
