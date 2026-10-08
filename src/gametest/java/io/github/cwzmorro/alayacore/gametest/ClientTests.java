package io.github.cwzmorro.alayacore.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.client.screen.PresetScreen;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;

final class ClientTests {
	static final int WIDE = 1920;
	static final int TALL = 1080;
	static final int TICKS_TO_SETTLE = 5;

	// Coordinates are relative to the preset panel's corner.
	static final int FIRST_ROW_X = 60;
	static final int FIRST_ROW_Y = 38;
	static final int FIRST_SLOT_X = 220;
	static final int FIRST_SLOT_Y = 55;
	static final int TAB_X = 288;
	static final int TAB_Y = 14;
	private static final int SETTINGS_HEADER_OFFSET = 14;
	private static final int SLIDER_HANDLE_INSET = 4;

	private ClientTests() {
	}

	static void setGuiScale(ClientGameTestContext context, int scale) {
		context.runOnClient(client -> {
			client.options.guiScale().set(scale);
			client.resizeGui();
		});
		context.waitTicks(TICKS_TO_SETTLE);
	}

	static double[] presetWindowPos(ClientGameTestContext context, int x, int y) {
		return context.computeOnClient(client -> {
			Button preset = client.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
				.filter(button -> button.getMessage().getString().equals("1")).findFirst().orElseThrow();
			int scale = client.getWindow().getGuiScale();
			return new double[]{(preset.getX() - PresetScreen.RIGHT_X + x) * scale, (preset.getY() - PresetScreen.PRESET_Y + y) * scale};
		});
	}

	static void moveToPanel(ClientGameTestContext context, int x, int y) {
		double[] pos = presetWindowPos(context, x, y);
		context.getInput().setCursorPos(pos[0], pos[1]);
	}

	static void clickPanel(ClientGameTestContext context, int x, int y) {
		moveToPanel(context, x, y);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	static void clickSlider(ClientGameTestContext context, int index, double fraction) {
		double[] pos = context.computeOnClient(client -> {
			AbstractSliderButton slider = sliders(client.screen.children()).get(index);
			int scale = client.getWindow().getGuiScale();
			return new double[]{(slider.getX() + SLIDER_HANDLE_INSET + fraction * (slider.getWidth() - 2 * SLIDER_HANDLE_INSET)) * scale,
				(slider.getY() + slider.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	static void closePresetSettings(ClientGameTestContext context) {
		double[] pos = context.computeOnClient(client -> {
			AbstractSliderButton slider = sliders(client.screen.children()).getFirst();
			int scale = client.getWindow().getGuiScale();
			return new double[]{(slider.getX() + slider.getWidth() / 2.0) * scale, (slider.getY() - SETTINGS_HEADER_OFFSET) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	private static List<AbstractSliderButton> sliders(List<? extends GuiEventListener> listeners) {
		return listeners.stream().filter(AbstractSliderButton.class::isInstance).map(AbstractSliderButton.class::cast).toList();
	}

	static void click(ClientGameTestContext context, String translationKey) {
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

	static @Nullable AbstractWidget find(List<? extends GuiEventListener> listeners, String translationKey) {
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
