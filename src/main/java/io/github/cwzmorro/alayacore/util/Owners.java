package io.github.cwzmorro.alayacore.util;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Resolves player or mob owners stored by UUID.
 */
public final class Owners {
	private Owners() {
	}

	/** Returns an online player on any level, then a living entity loaded in this level, or null if neither exists. */
	public static @Nullable LivingEntity find(ServerLevel level, @Nullable UUID id) {
		if (id == null) {
			return null;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
		if (player != null) {
			return player;
		}
		return level.getEntity(id) instanceof LivingEntity living ? living : null;
	}
}
