package io.github.cwzmorro.alayacore.client;

import io.github.cwzmorro.alayacore.client.hud.PresetHud;
import io.github.cwzmorro.alayacore.client.hud.ServantHud;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import io.github.cwzmorro.alayacore.client.screen.PresetScreen;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.config.ClientConfig;
import io.github.cwzmorro.alayacore.network.OpenServantSelectionPayload;
import io.github.cwzmorro.alayacore.network.ShieldConfigPayload;
import io.github.cwzmorro.alayacore.network.ShieldHitPayload;
import io.github.cwzmorro.alayacore.shield.ClientShieldEvents;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;

public final class AlayaCoreClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientConfig.load();
		ServantHud.register();
		PresetHud.register();
		AbilityKeys.register();
		ClientPlayNetworking.registerGlobalReceiver(OpenServantSelectionPayload.TYPE,
			(payload, context) -> context.client().setScreen(new ServantSelectionScreen(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ShieldConfigPayload.TYPE, (payload, context) -> {
			AlayaConfig.useServerShields(payload.shields());
			if (context.client().screen instanceof PresetScreen screen) {
				screen.refreshDescription();
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(ShieldHitPayload.TYPE, (payload, context) -> {
			ClientLevel level = context.client().level;
			if (level != null && level.getEntity(payload.shieldId()) instanceof ShieldEntity shield) {
				ClientShieldEvents.HIT.invoker().onHit(shield, payload.point(), payload.hpTaken());
			}
		});
	}
}
