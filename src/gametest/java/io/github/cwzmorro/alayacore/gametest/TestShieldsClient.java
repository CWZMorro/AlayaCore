package io.github.cwzmorro.alayacore.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

public final class TestShieldsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(TestShields.DISC, NoopRenderer::new);
		EntityRenderers.register(TestShields.SOLID_DISC, NoopRenderer::new);
		EntityRenderers.register(TestShields.DOME, NoopRenderer::new);
	}
}
