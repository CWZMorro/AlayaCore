package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.ROUND_TRIP_TICKS;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.player;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ServantScreenClientTest implements FabricClientGameTest {
	private static final String CHOOSE = "alayacore.screen.servant_selection.choose";
	private static final String NEXT = "alayacore.screen.servant_selection.next";
	private static final String CONTENTS = "alayacore.screen.servant_selection.contents";
	private static final int[] GUI_SCALES = {2, 3};
	private static final int PANEL_MIDDLE_ABOVE_CHOOSE = 80;
	// Layout of the window, from the buttons under it: the gap to the window, and from the window's top to the first text line's top.
	private static final int WINDOW_ABOVE_BUTTONS = ServantSelectionScreen.BUTTON_GAP;
	private static final int WINDOW_TOP_TO_FIRST_LINE = ServantSelectionScreen.WINDOW_PADDING + ServantSelectionScreen.HEADER_HEIGHT
		+ ServantSelectionScreen.HEADER_GAP + ServantSelectionScreen.PANEL_BORDER + ServantSelectionScreen.TEXT_PADDING;
	private static final int ARROW_CENTRE = ServantSelectionScreen.ARROW_HEIGHT / 2;
	// The contents lines: Human and Random, then a line per class in registry order; Archer is the second class.
	private static final int HUMAN_LINE = 0;
	private static final int ARCHER_LINE = 3;
	private static final double SCROLL_TO_END = -100.0;
	private static final double NEXT_SCROLL = -1.0;

	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();
			context.waitForScreen(ServantSelectionScreen.class);
			context.getInput().resizeWindow(ClientTests.WIDE, ClientTests.TALL);
			for (int scale : GUI_SCALES) {
				ClientTests.setGuiScale(context, scale);
				context.takeScreenshot("01-contents-scale-" + scale);
				ClientTests.click(context, NEXT);
				context.takeScreenshot("01-contents-page-2-scale-" + scale);
				ClientTests.click(context, CONTENTS);
			}
			check(!active(context, CHOOSE), "Choose active on the contents");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTick();
			check(context.computeOnClient(client -> client.screen instanceof ServantSelectionScreen), "Esc closed the first-join screen");
			check(world.getServer().computeOnServer(server -> speed(player(server)) == 0.0), "player not frozen while choosing");
			int pages = contentsPages(context);
			check(pages == 2, "the test classes should overflow the contents onto two pages, got " + pages);

			clickLine(context, ARCHER_LINE);
			check(active(context, CHOOSE), "the Archer line did not show a free servant");
			context.takeScreenshot("02-archer");
			ClientTests.click(context, CHOOSE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(context.computeOnClient(client -> client.screen == null), "screen still open after Choose");
			check(choice(world).equals(ServantChoice.servant(TestServants.ARCHER)), "not Test Archer: " + choice(world));
			check(world.getServer().computeOnServer(server -> speed(player(server)) > 0.0), "player still frozen");
			world.getClientLevel().waitForChunksRender();
			// Vanilla ignores clicks until player loading finishes after the paused selection screen.
			context.waitFor(client -> client.getConnection().hasClientLoaded());
			context.takeScreenshot("03-after-choose");

			world.getServer().runOnServer(server -> {
				Map<UUID, Identifier> owners = new HashMap<>(server.globalAttachments().getAttachedOrCreate(ServantStorage.OWNERS));
				owners.put(UUID.randomUUID(), TestServants.SOLO);
				owners.put(UUID.randomUUID(), TestServants.RANKED);
				server.globalAttachments().setAttached(ServantStorage.OWNERS, Map.copyOf(owners));
			});

			world.getServer().runCommand("give @a alayacore:spirit_origin_changer");
			context.waitTicks(ROUND_TRIP_TICKS);
			context.takeScreenshot("04-holding-changer");
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(ServantSelectionScreen.class);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(context.computeOnClient(client -> client.screen == null), "Esc did not close the reopened screen");
			check(choice(world).equals(ServantChoice.servant(TestServants.ARCHER)), "closing changed the servant: " + choice(world));
			check(!world.getServer().computeOnServer(server -> player(server).getAttachedOrElse(ServantStorage.CHOOSING, false)),
				"server still waits for an answer after closing");

			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(ServantSelectionScreen.class);
			moveTo(context, pages, 0, position(pages, servantIndex(TestServants.SOLO)));
			check(!active(context, CHOOSE), "Choose active on a taken servant");
			context.takeScreenshot("05-taken");
			moveCursorAway(context);
			context.getInput().scroll(NEXT_SCROLL);
			context.waitTick();
			check(active(context, CHOOSE), "the wheel outside the panel did not show the next servant");
			context.getInput().pressKey(InputConstants.KEY_LEFT);
			context.waitTick();
			check(!active(context, CHOOSE), "the left key did not go back to the taken servant");

			moveTo(context, pages, position(pages, servantIndex(TestServants.SOLO)), position(pages, servantIndex(TestServants.RANKED)));
			check(!active(context, CHOOSE), "Choose active on the taken servant with abilities");
			context.takeScreenshot("06-taken-abilities");
			context.getInput().setCursorPos(panelMiddle(context)[0], panelMiddle(context)[1]);
			context.getInput().scroll(SCROLL_TO_END);
			context.waitTick();
			check(!active(context, CHOOSE), "the wheel over the panel changed the servant");
			context.takeScreenshot("07-scrolled");

			moveTo(context, pages, position(pages, servantIndex(TestServants.RANKED)), pages + 1);
			context.takeScreenshot("08-random");
			context.getInput().pressKey(InputConstants.KEY_LEFT);
			context.waitTick();
			context.takeScreenshot("09-human");
			ClientTests.click(context, CONTENTS);
			check(!active(context, CHOOSE), "Choose active on the contents");
			context.getInput().pressKey(InputConstants.KEY_LEFT);
			context.waitTick();
			context.takeScreenshot("10-wrapped-to-last");
			ClientTests.click(context, CONTENTS);
			clickLine(context, HUMAN_LINE);
			check(active(context, CHOOSE), "the Human line did not show Human");
			ClientTests.click(context, CHOOSE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(choice(world).equals(ServantChoice.human()), "not human: " + choice(world));

			world.getServer().runCommand("gamerule alayacore:force_random_servant true");
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(ServantSelectionScreen.class);
			context.takeScreenshot("11-force-random-contents");
			ClientTests.click(context, NEXT);
			ClientTests.click(context, NEXT);
			context.takeScreenshot("11-force-random");
			ClientTests.click(context, CHOOSE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(choice(world).servant().isPresent(), "no servant after Random: " + choice(world));
			context.takeScreenshot("12-random-result-in-chat");
			world.getServer().runCommand("gamerule alayacore:force_random_servant false");

			world.getServer().runCommand("alayacore config reload");
			context.waitTicks(ROUND_TRIP_TICKS);
			context.takeScreenshot("13-config-reload");
		}

		try (TestSingleplayerContext world = save.open()) {
			world.getClientLevel().waitForChunksRender();
			check(context.computeOnClient(client -> client.screen == null), "screen opened again on rejoin");
			check(choice(world).servant().isPresent(), "choice lost on rejoin: " + choice(world));
			context.takeScreenshot("14-rejoin");
		}
	}

	private static int servantIndex(Identifier servant) {
		int index = 0;
		for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
			for (Servant candidate : AlayaRegistries.SERVANT) {
				if (candidate.servantClass().identifier().equals(AlayaRegistries.SERVANT_CLASS.getKey(servantClass))) {
					if (AlayaRegistries.SERVANT.getKey(candidate).equals(servant)) {
						return index;
					}
					index++;
				}
			}
		}
		throw new AssertionError("no such servant: " + servant);
	}

	private static int position(int contentsPages, int servantIndex) {
		return contentsPages + 2 + servantIndex;
	}

	private static int contentsPages(ClientGameTestContext context) {
		ClientTests.click(context, CONTENTS);
		int pages = 0;
		while (!active(context, CHOOSE)) {
			ClientTests.click(context, NEXT);
			pages++;
		}
		ClientTests.click(context, CONTENTS);
		return pages;
	}

	private static void moveTo(ClientGameTestContext context, int pages, int from, int to) {
		int entries = position(pages, servantCount());
		for (int i = 0; i < Math.floorMod(to - from, entries); i++) {
			ClientTests.click(context, NEXT);
		}
	}

	private static int servantCount() {
		return AlayaRegistries.SERVANT.size();
	}

	private static double speed(ServerPlayer player) {
		return player.getAttributeValue(Attributes.MOVEMENT_SPEED);
	}

	private static ServantChoice choice(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> player(server).getAttachedOrCreate(ServantStorage.CHOICE));
	}

	private static boolean active(ClientGameTestContext context, String translationKey) {
		return context.computeOnClient(client -> Objects.requireNonNull(ClientTests.find(client.screen.children(), translationKey), translationKey).active);
	}

	private static void clickLine(ClientGameTestContext context, int line) {
		double[] pos = context.computeOnClient(client -> {
			AbstractWidget choose = Objects.requireNonNull(ClientTests.find(client.screen.children(), CHOOSE), CHOOSE);
			AbstractWidget next = Objects.requireNonNull(ClientTests.find(client.screen.children(), NEXT), NEXT);
			int windowHeight = 2 * (choose.getY() - WINDOW_ABOVE_BUTTONS - (next.getY() + ARROW_CENTRE));
			int windowTop = choose.getY() - WINDOW_ABOVE_BUTTONS - windowHeight;
			int scale = client.getWindow().getGuiScale();
			int lineHeight = client.font.lineHeight;
			int lineMiddle = WINDOW_TOP_TO_FIRST_LINE + lineHeight / 2 + line * (lineHeight + ServantSelectionScreen.LINE_SPACING);
			return new double[]{(choose.getX() + choose.getWidth() / 2.0) * scale, (windowTop + lineMiddle) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	private static double[] panelMiddle(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			AbstractWidget choose = Objects.requireNonNull(ClientTests.find(client.screen.children(), CHOOSE), CHOOSE);
			int scale = client.getWindow().getGuiScale();
			return new double[]{(choose.getX() + choose.getWidth() / 2.0) * scale, (choose.getY() - PANEL_MIDDLE_ABOVE_CHOOSE) * scale};
		});
	}

	private static void moveCursorAway(ClientGameTestContext context) {
		context.getInput().setCursorPos(0.0, 0.0);
	}
}
