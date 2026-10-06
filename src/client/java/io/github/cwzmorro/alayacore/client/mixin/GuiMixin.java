package io.github.cwzmorro.alayacore.client.mixin;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// The HUD replaces hearts, so armor must not be offset by the hidden heart rows.
@Mixin(Gui.class)
abstract class GuiMixin {
	@Unique
	private static final String alayacore$extractArmor =
		"Lnet/minecraft/client/gui/Gui;extractArmor(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIII)V";
	// Vanilla places armor (rows - 1) × rowHeight + 10 above the bottom heart row; 0 rows of 10 puts it on that row.
	@Unique
	private static final int alayacore$noHeartRows = 0;
	@Unique
	private static final int alayacore$heartRowHeight = 10;

	@ModifyArg(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = alayacore$extractArmor), index = 3)
	private int alayacore$noHeartRows(int numHealthRows) {
		return alayacore$noHeartRows;
	}

	@ModifyArg(method = "extractPlayerHealth", at = @At(value = "INVOKE", target = alayacore$extractArmor), index = 4)
	private int alayacore$heartRowHeight(int healthRowHeight) {
		return alayacore$heartRowHeight;
	}
}
