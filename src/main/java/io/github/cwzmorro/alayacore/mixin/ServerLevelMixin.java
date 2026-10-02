package io.github.cwzmorro.alayacore.mixin;

import io.github.cwzmorro.alayacore.mana.ManaEvents;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mana for sleeping through the night (plan 7). Vanilla only calls {@code wakeUpAllPlayers} when
 * enough players slept for the night to be skipped; a player who just gets out of bed is not counted.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
	@Shadow
	public abstract List<ServerPlayer> players();

	@Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
	private void alayacore$manaFromSleep(CallbackInfo ci) {
		for (ServerPlayer player : this.players()) {
			if (player.isSleeping()) {
				ManaEvents.onSleptThroughNight(player);
			}
		}
	}
}
