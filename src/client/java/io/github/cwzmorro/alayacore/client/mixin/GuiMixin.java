package io.github.cwzmorro.alayacore.client.mixin;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla's hearts are hidden (the HUD shows HP), so armor moves down into the hearts' row instead
 * of sitting above rows of hearts that are no longer drawn (servants have 300–500 HP).
 */
@Mixin(Gui.class)
abstract class GuiMixin {
	private static final String EXTRACT_ARMOR =
		"Lnet/minecraft/client/gui/Gui;extractArmor(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIII)V";
	/** Vanilla places armor {@code (rows - 1) × rowHeight + 10} above the bottom heart row; 0 rows of 10 puts it on that row. */
	private static final int NO_HEART_ROWS = 0;
	private static final int HEART_ROW_HEIGHT = 10;

	@ModifyArg(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = EXTRACT_ARMOR), index = 3)
	private int alayacore$noHeartRows(int numHealthRows) {
		return NO_HEART_ROWS;
	}

	@ModifyArg(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = EXTRACT_ARMOR), index = 4)
	private int alayacore$heartRowHeight(int healthRowHeight) {
		return HEART_ROW_HEIGHT;
	}
}
