package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.ROUND_TRIP_TICKS;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.player;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jspecify.annotations.Nullable;

/**
 * Plays through the servant selection book in a real client (plan 3): first join, Esc, the contents
 * (two pages with the extra test classes), Contents links, class pages, an empty class, Random and Stay
 * human, choosing, the Spirit Origin Changer (Esc closes it), a taken servant, staying human, force random,
 * the config reload command, and rejoining. Screenshots go to the run folder's {@code screenshots/}.
 */
public class ServantScreenClientTest implements FabricClientGameTest {
	private static final String CHOOSE = "alayacore.screen.servant_selection.choose";
	private static final String CONTENTS = "alayacore.screen.servant_selection.contents";
	private static final String RANDOM = "alayacore.screen.servant_selection.random.title";
	private static final String HUMAN = "alayacore.screen.servant_selection.human.title";
	private static final String NEXT_PAGE = "book.page_button.next";
	private static final List<String> CLASSES = List.of("saber", "archer", "lancer", "rider", "caster", "assassin", "berserker", "ruler", "shielder");

	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			save = world.getWorldSave();

			// First join: the book opens on the contents, pauses the game, and Esc does not close it.
			context.waitForScreen(ServantSelectionScreen.class);
			context.takeScreenshot("01-contents");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTick();
			check(context.computeOnClient(client -> client.screen instanceof ServantSelectionScreen), "Esc closed the first-join screen");
			check(world.getServer().computeOnServer(server -> speed(player(server)) == 0.0), "player not frozen while choosing");
			for (String c : CLASSES) {
				check(find(context, "servant_class.alayacore." + c) != null, "no contents line for " + c);
			}

			// The extra test classes push Random and Human onto a second contents page.
			check(find(context, RANDOM) == null, "everything fit on one contents page");
			click(context, NEXT_PAGE);
			check(find(context, RANDOM) != null && find(context, HUMAN) != null, "no Random / Human line on contents page 2");
			context.takeScreenshot("02-contents-page-2");

			// A class page lists its servants; a class without servants says Coming soon.
			click(context, CONTENTS);
			click(context, "servant_class.alayacore.saber");
			check(find(context, "servant.alayacore-gametest.solo") != null, "Saber's page does not list Test Solo");
			check(!active(context, CHOOSE), "Choose active on a class page");
			context.takeScreenshot("03-saber-class");
			click(context, CONTENTS);
			click(context, "servant_class.alayacore.lancer");
			context.takeScreenshot("04-lancer-coming-soon");

			click(context, CONTENTS);
			click(context, NEXT_PAGE);
			click(context, RANDOM);
			context.takeScreenshot("05-random-page");
			click(context, CONTENTS);
			click(context, NEXT_PAGE);
			click(context, HUMAN);
			context.takeScreenshot("06-human-page");

			// Choose a servant: the screen closes, the player is that servant and can move again.
			click(context, CONTENTS);
			click(context, "servant_class.alayacore.archer");
			click(context, "servant.alayacore-gametest.archer");
			context.takeScreenshot("07-archer-page");
			click(context, CHOOSE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(context.computeOnClient(client -> client.screen == null), "screen still open after Choose");
			check(choice(world).equals(ServantChoice.servant(TestServants.ARCHER)), "not Test Archer: " + choice(world));
			check(world.getServer().computeOnServer(server -> speed(player(server)) > 0.0), "player still frozen");
			world.getClientLevel().waitForChunksRender();
			// The first-join book paused the game, so vanilla only now finishes loading the player in; until then it ignores clicks.
			context.waitFor(client -> client.getConnection().hasClientLoaded());
			context.takeScreenshot("08-after-choose");

			// Someone else owns Test Solo: its page says Taken and Choose is disabled.
			world.getServer().runOnServer(server -> {
				Map<UUID, Identifier> owners = new HashMap<>(server.globalAttachments().getAttachedOrCreate(ServantStorage.OWNERS));
				owners.put(UUID.randomUUID(), TestServants.SOLO);
				server.globalAttachments().setAttached(ServantStorage.OWNERS, Map.copyOf(owners));
			});

			// The Spirit Origin Changer reopens the book; this time Esc closes it and nothing changes.
			world.getServer().runCommand("give @a alayacore:spirit_origin_changer");
			context.waitTicks(ROUND_TRIP_TICKS);
			context.takeScreenshot("09-holding-changer");
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(ServantSelectionScreen.class);
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(context.computeOnClient(client -> client.screen == null), "Esc did not close the reopened book");
			check(choice(world).equals(ServantChoice.servant(TestServants.ARCHER)), "closing changed the servant: " + choice(world));
			check(!world.getServer().computeOnServer(server -> player(server).getAttachedOrElse(ServantStorage.CHOOSING, false)),
				"server still waits for an answer after closing");

			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(ServantSelectionScreen.class);
			click(context, "servant_class.alayacore.saber");
			click(context, "servant.alayacore-gametest.solo");
			check(!active(context, CHOOSE), "Choose active on a taken servant");
			context.takeScreenshot("10-taken");

			// Human.
			click(context, CONTENTS);
			click(context, NEXT_PAGE);
			click(context, HUMAN);
			click(context, CHOOSE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(choice(world).equals(ServantChoice.human()), "not human: " + choice(world));

			// Force random: only Random and Human.
			world.getServer().runCommand("gamerule alayacore:force_random_servant true");
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(ServantSelectionScreen.class);
			check(find(context, "servant_class.alayacore.archer") == null, "classes listed under force random");
			context.takeScreenshot("11-force-random");
			click(context, RANDOM);
			click(context, CHOOSE);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(choice(world).servant().isPresent(), "no servant after Random: " + choice(world));
			context.takeScreenshot("12-random-result-in-chat");
			world.getServer().runCommand("gamerule alayacore:force_random_servant false");

			// Config reload command.
			world.getServer().runCommand("alayacore config reload");
			context.waitTicks(ROUND_TRIP_TICKS);
			context.takeScreenshot("13-config-reload");
		}

		// Rejoining the same world: the choice is kept, no screen.
		try (TestSingleplayerContext world = save.open()) {
			world.getClientLevel().waitForChunksRender();
			check(context.computeOnClient(client -> client.screen == null), "screen opened again on rejoin");
			check(choice(world).servant().isPresent(), "choice lost on rejoin: " + choice(world));
			context.takeScreenshot("14-rejoin");
		}
	}

	private static double speed(ServerPlayer player) {
		return player.getAttributeValue(Attributes.MOVEMENT_SPEED);
	}

	private static ServantChoice choice(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> player(server).getAttachedOrCreate(ServantStorage.CHOICE));
	}

	/** Whether the widget showing this translation key is active (clickable). */
	private static boolean active(ClientGameTestContext context, String translationKey) {
		return context.computeOnClient(client -> Objects.requireNonNull(find(client.screen.children(), translationKey), translationKey).active);
	}

	private static @Nullable AbstractWidget find(ClientGameTestContext context, String translationKey) {
		return context.computeOnClient(client -> find(client.screen.children(), translationKey));
	}

	/**
	 * Moves the cursor onto the widget showing this translation key and left-clicks, like a player.
	 * Unlike {@code clickScreenButton}, this also finds contents lines and page arrows.
	 */
	private static void click(ClientGameTestContext context, String translationKey) {
		double[] windowPos = context.computeOnClient(client -> {
			AbstractWidget widget = find(client.screen.children(), translationKey);
			if (widget == null) {
				throw new AssertionError("No widget '" + translationKey + "' on " + client.screen);
			}
			int scale = client.getWindow().getGuiScale();
			return new double[]{(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(windowPos[0], windowPos[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	private static @Nullable AbstractWidget find(List<? extends GuiEventListener> listeners, String translationKey) {
		for (GuiEventListener listener : listeners) {
			if (listener instanceof AbstractWidget widget && widget.visible
				&& widget.getMessage().getContents() instanceof TranslatableContents contents && contents.getKey().equals(translationKey)) {
				return widget;
			}
			if (listener instanceof ContainerEventHandler container) {
				AbstractWidget found = find(container.children(), translationKey);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
