package io.github.cwzmorro.alayacore.servant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/** Who may take which servant (plan 3). Plain Java, no game calls. */
public final class ServantRules {
	private ServantRules() {
	}

	/**
	 * A servant is free for {@code player} if duplicates are allowed, or nobody else has it.
	 * The player's own current servant counts as free for them.
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

	/** The servants {@code player} may take, in the given order. */
	public static List<Identifier> free(Collection<Identifier> servants, Map<UUID, Identifier> owners, UUID player, boolean allowDuplicates) {
		return servants.stream().filter(servant -> isFree(servant, owners, player, allowDuplicates)).toList();
	}
}
