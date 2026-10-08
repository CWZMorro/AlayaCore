package io.github.cwzmorro.alayacore.shield;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.phys.Vec3;

/**
 * Exposes client-side shield hit notifications for content mod visuals.
 */
public final class ClientShieldEvents {
	/** Fires when a tracked shield loses HP, with the world-space impact and HP lost. */
	public static final Event<Hit> HIT = EventFactory.createArrayBacked(Hit.class, listeners -> (shield, point, hpTaken) -> {
		for (Hit listener : listeners) {
			listener.onHit(shield, point, hpTaken);
		}
	});

	private ClientShieldEvents() {
	}

	@FunctionalInterface
	public interface Hit {
		void onHit(ShieldEntity shield, Vec3 point, float hpTaken);
	}
}
