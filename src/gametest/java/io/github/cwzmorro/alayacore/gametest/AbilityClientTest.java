package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestAbilities.event;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.ROUND_TRIP_TICKS;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.player;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.servant;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.ability.AbilityPresets;
import io.github.cwzmorro.alayacore.ability.Presets;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import io.github.cwzmorro.alayacore.client.screen.PresetScreen;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

/**
 * A real player uses the controls (plan 8): the preset screen (B), filling a slot and changing its
 * mode, setting a quick-cast key there, the slot key Z held and tapped, the mode key (Left Alt) with the
 * scroll wheel, 1–5 and Z, and the quick-cast key's name and use. Screenshots go to the run folder's {@code screenshots/}.
 */
public class AbilityClientTest implements FabricClientGameTest {
	private static final Identifier RECORDER = TestAbilities.RECORDER.identifier();
	// Layout of PresetScreen (its 276 × 196 panel is centred): first list row, first slot, preset buttons,
	// the second quick-cast key button.
	private static final int PANEL_WIDTH = 276;
	private static final int PANEL_HEIGHT = 196;
	private static final int FIRST_ROW_X = 60;
	private static final int FIRST_ROW_Y = 38;
	private static final int FIRST_SLOT_X = 220;
	private static final int FIRST_SLOT_Y = 55;
	private static final int SECOND_QUICK_CAST_X = 118;
	private static final int SECOND_QUICK_CAST_Y = 157;
	private static final int PRESET_BUTTON_X = 166;
	private static final int PRESET_BUTTON_SPACING = 22;
	private static final int PRESET_BUTTON_Y = 28;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(ServantSelectionScreen.class);
			world.getServer().runOnServer(server -> ServantSelection.choose(player(server), servant(TestServants.ARCHER)));
			context.setScreen(() -> null);
			world.getClientLevel().waitForChunksRender();

			// The preset screen: place the Recorder in slot Z, scroll to its second mode, try another preset.
			context.getInput().pressKey(InputConstants.KEY_B);
			context.waitForScreen(PresetScreen.class);
			context.takeScreenshot("presets-01-screen");
			click(context, FIRST_ROW_X, FIRST_ROW_Y);
			click(context, FIRST_SLOT_X, FIRST_SLOT_Y);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(slot(world, 0).isPresent(), "Recorder not placed in slot Z");
			moveTo(context, FIRST_SLOT_X, FIRST_SLOT_Y);
			context.getInput().scroll(-1);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(slot(world, 0).orElseThrow().mode() == 1, "scroll did not change the mode: " + slot(world, 0));
			context.takeScreenshot("presets-02-filled");
			click(context, PRESET_BUTTON_X + PRESET_BUTTON_SPACING, PRESET_BUTTON_Y);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(active(world) == 1, "preset button 2 did not select preset 2");
			click(context, PRESET_BUTTON_X, PRESET_BUTTON_Y);
			context.waitTicks(ROUND_TRIP_TICKS);
			// The Recorder's second mode gets a quick-cast key here, the same key as in Controls.
			click(context, SECOND_QUICK_CAST_X, SECOND_QUICK_CAST_Y);
			context.getInput().pressKey(InputConstants.KEY_V);
			context.waitTick();
			check(context.computeOnClient(client -> AbilityKeys.quickCastKey(RECORDER, 1).orElseThrow().getTranslatedKeyMessage().getString())
				.equals("V"), "quick-cast key not bound to V");
			context.takeScreenshot("presets-02b-quick-cast");
			context.getInput().pressKey(InputConstants.KEY_B);
			context.waitTick();
			check(context.computeOnClient(client -> client.screen == null), "B did not close the preset screen");
			context.takeScreenshot("presets-03-hud");

			// Z held past the threshold, then tapped and pressed again.
			int threshold = context.computeOnClient(client -> AlayaConfig.abilities().holdThresholdTicks());
			context.getInput().holdKeyFor(InputConstants.KEY_Z, threshold + ROUND_TRIP_TICKS);
			context.waitTicks(ROUND_TRIP_TICKS);
			List<String> held = events(world);
			check(held.contains(event(TestAbilities.RECORDER, "hold_start")) && held.getLast().equals(event(TestAbilities.RECORDER, "end")),
				"Z held is no hold: " + held);
			context.getInput().pressKey(InputConstants.KEY_Z);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(events(world).getLast().equals(event(TestAbilities.RECORDER, "tap")), "Z tapped is no tap: " + events(world));
			context.getInput().pressKey(InputConstants.KEY_Z);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(events(world).getLast().equals(event(TestAbilities.RECORDER, "end")), "second Z did not end it: " + events(world));

			// The mode key: + scroll and + 1–5 change the preset (not the hotbar), + Z changes Z's mode.
			int hotbarSlot = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
			context.getInput().holdAlt();
			context.getInput().scroll(-1);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(active(world) == 1, "Alt + scroll did not move to preset 2");
			context.getInput().pressKey(options -> options.keyHotbarSlots[2]);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(active(world) == 2, "Alt + 3 did not select preset 3");
			context.getInput().pressKey(options -> options.keyHotbarSlots[0]);
			context.waitTicks(ROUND_TRIP_TICKS);
			context.getInput().pressKey(InputConstants.KEY_Z);
			context.waitTicks(ROUND_TRIP_TICKS);
			context.getInput().releaseAlt();
			check(context.computeOnClient(client -> client.player.getInventory().getSelectedSlot()) == hotbarSlot, "the hotbar moved");
			check(active(world) == 0 && slot(world, 0).orElseThrow().mode() == 0, "Alt + Z did not change Z's mode: " + slot(world, 0));
			check(!events(world).getLast().equals(event(TestAbilities.RECORDER, "start")), "Alt + Z also cast the ability");

			String quickCast = context.computeOnClient(client -> I18n.get("key.alayacore.quick_cast.alayacore-gametest.recorder.1"));
			check(quickCast.equals("Recorder: Second mode"), "quick-cast key name: " + quickCast);
			context.getInput().pressKey(InputConstants.KEY_V);
			context.waitTicks(ROUND_TRIP_TICKS);
			context.getInput().pressKey(InputConstants.KEY_V);
			context.waitTicks(ROUND_TRIP_TICKS);
			check(events(world).getLast().equals(event(TestAbilities.RECORDER, "end")), "quick-cast V did not cast: " + events(world));
		}
	}

	/** Window position of a point given relative to the preset screen's panel. */
	private static double[] windowPos(ClientGameTestContext context, int x, int y) {
		return context.computeOnClient(client -> {
			int scale = client.getWindow().getGuiScale();
			return new double[]{((client.screen.width - PANEL_WIDTH) / 2 + x) * scale, ((client.screen.height - PANEL_HEIGHT) / 2 + y) * scale};
		});
	}

	private static void moveTo(ClientGameTestContext context, int x, int y) {
		double[] pos = windowPos(context, x, y);
		context.getInput().setCursorPos(pos[0], pos[1]);
	}

	private static void click(ClientGameTestContext context, int x, int y) {
		moveTo(context, x, y);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	private static AbilityPresets presets(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> Presets.get(player(server)));
	}

	private static Optional<AbilityPresets.Slot> slot(TestSingleplayerContext world, int slot) {
		AbilityPresets presets = presets(world);
		return presets.slot(presets.active(), slot);
	}

	private static int active(TestSingleplayerContext world) {
		return presets(world).active();
	}

	private static List<String> events(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> TestAbilities.events(player(server)));
	}
}
