package io.github.cwzmorro.alayacore.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Stores side-by-side image copies at increasing integer scales to avoid resampling at common GUI scales.
 * Stretches the largest copy at higher scales.
 *
 * @param size   the image's size in GUI pixels, which is the size of the first copy
 * @param copies how many copies the texture holds
 */
public record StripTexture(int size, int copies) {
	public void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int screenPixelsPerTexel) {
		int copy = Math.min(screenPixelsPerTexel, this.copies);
		int copySize = this.size * copy;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, this.size * copy * (copy - 1) / 2.0F, 0.0F, this.size, this.size,
			copySize, copySize, this.size * this.copies * (this.copies + 1) / 2, this.size * this.copies);
	}
}
