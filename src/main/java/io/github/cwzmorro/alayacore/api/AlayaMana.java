package io.github.cwzmorro.alayacore.api;

import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.mana.ManaRules;
import io.github.cwzmorro.alayacore.mana.ManaStorage;
import io.github.cwzmorro.alayacore.progression.Progression;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Provides entity mana access; mutations are server-only.
 * Players receive mana on joining; other entities have none until it is created explicitly.
 */
public final class AlayaMana {
	private AlayaMana() {
	}

	public static boolean hasMana(LivingEntity entity) {
		return entity.hasAttached(ManaStorage.MANA);
	}

	/** Returns the entity's mana, or null if it has none. */
	public static @Nullable ManaData get(LivingEntity entity) {
		return entity.getAttached(ManaStorage.MANA);
	}

	/** Returns the entity's mana, creating it from the configured defaults if absent. */
	public static ManaData getOrCreate(LivingEntity entity) {
		return entity.getAttachedOrCreate(ManaStorage.MANA);
	}

	/** Applies a change if the entity has mana; does nothing otherwise. Unchanged values are not re-sent. */
	public static void update(LivingEntity entity, UnaryOperator<ManaData> change) {
		ManaData before = get(entity);
		if (before == null) {
			return;
		}
		ManaData after = change.apply(before);
		if (!after.equals(before)) {
			entity.setAttached(ManaStorage.MANA, after);
			// Sync comes from max mana.
			if (after.max() != before.max() && entity instanceof ServerPlayer player) {
				Progression.refresh(player);
			}
		}
	}

	public static void addCurrent(LivingEntity entity, double amount) {
		update(entity, mana -> ManaRules.addCurrent(mana, amount));
	}

	public static void addMax(LivingEntity entity, double amount) {
		update(entity, mana -> ManaRules.addMax(mana, amount));
	}
}
