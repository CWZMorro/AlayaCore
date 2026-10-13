package io.github.cwzmorro.alayacore.shield;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.EntityType;
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

	/** Registers a hit listener filtered to the given shield type. */
	public static <T extends ShieldEntity> void register(EntityType<T> type, Look<T> look) {
		HIT.register((shield, point, hpTaken) -> {
			T hit = type.tryCast(shield);
			if (hit != null) {
				look.onHit(hit, point, hpTaken);
			}
		});
	}

	@FunctionalInterface
	public interface Hit {
		void onHit(ShieldEntity shield, Vec3 point, float hpTaken);
	}

	@FunctionalInterface
	public interface Look<T extends ShieldEntity> {
		void onHit(T shield, Vec3 point, float hpTaken);
	}
}
