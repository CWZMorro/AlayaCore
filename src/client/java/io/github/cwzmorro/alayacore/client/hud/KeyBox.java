package io.github.cwzmorro.alayacore.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** A preset slot's key in vanilla's slot box: drawn on the preset HUD and beside each slot of the preset screen. */
public final class KeyBox {
	public static final int SIZE = 18;
	private static final Identifier SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int TEXT = 0xFFFFFFFF;

	private KeyBox() {
	}

	public static void extract(GuiGraphicsExtractor graphics, Font font, int x, int y, Component key, float opacity) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, x, y, SIZE, SIZE, opacity);
		graphics.centeredText(font, key, x + SIZE / 2, centredTextY(font, y, SIZE), ARGB.multiplyAlpha(TEXT, opacity));
	}

	// A pixel lower than the middle of the line's height, because the font's glyphs sit above its baseline row.
	public static int centredTextY(Font font, int boxY, int height) {
		return boxY + (height - font.lineHeight) / 2 + 1;
	}
}
