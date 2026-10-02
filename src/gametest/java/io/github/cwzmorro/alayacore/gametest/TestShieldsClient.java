package io.github.cwzmorro.alayacore.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** The test shields draw nothing; every entity type needs a renderer on the client. */
public final class TestShieldsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(TestShields.DISC, NoopRenderer::new);
		EntityRenderers.register(TestShields.DOME, NoopRenderer::new);
	}
}
