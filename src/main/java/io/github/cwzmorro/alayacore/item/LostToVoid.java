package io.github.cwzmorro.alayacore.item;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;

/**
 * What happens to a dropped item that falls into the void: a content mod registers a handler for its own items (an
 * item that goes back to its owner, one that can generate again); other items are simply lost.
 */
public final class LostToVoid {
	private static final Map<Item, Handler> HANDLERS = new HashMap<>();

	/** A content mod's response to one of its items falling out of the world; the entity is removed by vanilla right after. */
	@FunctionalInterface
	public interface Handler {
		void lost(ServerLevel level, ItemEntity entity);
	}

	private LostToVoid() {
	}

	public static void register(Item item, Handler handler) {
		if (HANDLERS.putIfAbsent(item, handler) != null) {
			throw new IllegalStateException("A void handler is already registered for " + item);
		}
	}

	public static void lost(ServerLevel level, ItemEntity entity) {
		Handler handler = HANDLERS.get(entity.getItem().getItem());
		if (handler != null) {
			handler.lost(level, entity);
		}
	}
}
