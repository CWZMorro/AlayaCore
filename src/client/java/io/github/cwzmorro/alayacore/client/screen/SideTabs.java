package io.github.cwzmorro.alayacore.client.screen;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.client.hud.KeyBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Draws settings tabs using vanilla advancement sprites. */
public final class SideTabs {
	private static final Identifier TAB_RIGHT = Identifier.withDefaultNamespace("advancements/tab_right_top");
	private static final Identifier COG = AlayaCore.id("settings");

	public static final int WIDTH = 32;
	public static final int HEIGHT = 28;
	public static final int UNDER = 4;
	public static final int FRAME = 3;
	public static final int RECESS = 4;
	public static final int BODY_HEIGHT = HEIGHT - 2 * FRAME;
	public static final int COG_SIZE = 16;
	public static final int SETTINGS_WIDTH = 154;

	private SideTabs() {
	}

	public static ScreenRectangle settingsTab(int panelX, int panelY, int panelWidth) {
		return new ScreenRectangle(panelX + panelWidth - UNDER, panelY, WIDTH, HEIGHT);
	}

	public static ScreenRectangle settingsArea(int panelX, int panelY, int panelWidth, int height, int screenWidth, int screenHeight, int margin) {
		return settingsArea(panelX, panelY, panelWidth, SETTINGS_WIDTH, height, screenWidth, screenHeight, margin);
	}

	public static ScreenRectangle settingsArea(int panelX, int panelY, int panelWidth, int width, int height, int screenWidth, int screenHeight, int margin) {
		int x = Math.clamp(panelX + panelWidth - UNDER, margin, Math.max(margin, screenWidth - margin - width));
		int y = Math.clamp(panelY, margin, Math.max(margin, screenHeight - margin - height));
		return new ScreenRectangle(x, y, width, height);
	}

	public static void extractSettingsTab(GuiGraphicsExtractor graphics, ScreenRectangle tab, int guiScale) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TAB_RIGHT, tab.left(), tab.top(), WIDTH, HEIGHT);
		int bodyWidth = WIDTH - RECESS - FRAME - UNDER;
		graphics.pose().pushMatrix();
		graphics.pose().translate(onScreenPixel(tab.left() + UNDER + (bodyWidth - COG_SIZE) / 2.0F, guiScale),
			onScreenPixel(tab.top() + FRAME + (BODY_HEIGHT - COG_SIZE) / 2.0F, guiScale));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COG, 0, 0, COG_SIZE, COG_SIZE);
		graphics.pose().popMatrix();
	}

	public static void extractSettingsTitle(GuiGraphicsExtractor graphics, Font font, ScreenRectangle area, int edge, int gap,
		Component title, int color) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COG, area.left() + edge, area.top() + edge, COG_SIZE, COG_SIZE);
		int titleX = area.left() + edge + COG_SIZE + gap;
		graphics.text(font, font.substrByWidth(title, area.right() - edge - titleX).getString(), titleX,
			KeyBox.centredTextY(font, area.top() + edge, COG_SIZE), color, false);
	}

	public static float onScreenPixel(float gui, int guiScale) {
		return Math.round(gui * guiScale) / (float) guiScale;
	}
}
