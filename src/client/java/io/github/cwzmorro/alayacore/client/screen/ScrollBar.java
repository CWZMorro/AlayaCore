package io.github.cwzmorro.alayacore.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

final class ScrollBar {
	static final int WIDTH = 6;
	private static final int THUMB_INSET = 1;
	private static final int MIN_THUMB_HEIGHT = 12;

	private ScrollBar() {
	}

	static int wheelLines(double scrollY) {
		int notches = Math.max(1, (int) Math.ceil(Math.abs(scrollY)));
		return scrollY < 0 ? notches : -notches;
	}

	static void extract(GuiGraphicsExtractor graphics, int x, int y, int height, int visible, int total, int scroll) {
		int thumbHeight = thumbHeight(height, visible, total);
		int thumbY = y + THUMB_INSET + travel(height, visible, total) * scroll / Math.max(1, total - visible);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiSprites.SCROLL_TRACK, x, y, WIDTH, height);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiSprites.SCROLL_THUMB, x + THUMB_INSET, thumbY, WIDTH - 2 * THUMB_INSET, thumbHeight);
	}

	static int scrollAt(double mouseY, int y, int height, int visible, int total) {
		double fraction = (mouseY - y - THUMB_INSET - thumbHeight(height, visible, total) / 2.0) / travel(height, visible, total);
		return Math.clamp(Math.round(fraction * (total - visible)), 0, Math.max(0, total - visible));
	}

	private static int thumbHeight(int height, int visible, int total) {
		return Math.max(MIN_THUMB_HEIGHT, (height - 2 * THUMB_INSET) * visible / Math.max(visible, total));
	}

	private static int travel(int height, int visible, int total) {
		return height - 2 * THUMB_INSET - thumbHeight(height, visible, total);
	}
}
