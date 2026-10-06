package io.github.cwzmorro.alayacore.servant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/** Who may take which servant. No game calls. */
public final class ServantRules {
	private ServantRules() {
	}

	/**
	 * Returns whether duplicates are allowed or no other player owns the servant; the player's own servant counts as free.
	 */
	public static boolean isFree(Identifier servant, Map<UUID, Identifier> owners, UUID player, boolean allowDuplicates) {
		if (allowDuplicates) {
			return true;
		}
		for (Map.Entry<UUID, Identifier> owner : owners.entrySet()) {
			if (owner.getValue().equals(servant) && !owner.getKey().equals(player)) {
				return false;
			}
		}
		return true;
	}

	public static List<Identifier> free(Collection<Identifier> servants, Map<UUID, Identifier> owners, UUID player, boolean allowDuplicates) {
		return servants.stream().filter(servant -> isFree(servant, owners, player, allowDuplicates)).toList();
	}
}
