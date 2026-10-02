package io.github.cwzmorro.alayacore.client;

import io.github.cwzmorro.alayacore.client.hud.PresetHud;
import io.github.cwzmorro.alayacore.client.hud.ServantHud;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import io.github.cwzmorro.alayacore.client.screen.ServantSelectionScreen;
import io.github.cwzmorro.alayacore.network.OpenServantSelectionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entrypoint: screens, HUD and drawing only. The server decides everything. */
public final class AlayaCoreClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ServantHud.register();
		PresetHud.register();
		AbilityKeys.register();
		ClientPlayNetworking.registerGlobalReceiver(OpenServantSelectionPayload.TYPE,
			(payload, context) -> context.client().setScreen(new ServantSelectionScreen(payload)));
	}
}
