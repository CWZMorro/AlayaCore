package io.github.cwzmorro.alayacore.marble;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Supplies a content mod's live marbles to {@link RealityMarbles}; register once during initialization.
 */
public interface RealityMarbleSource {
	/** Returns every marble on the server, including broken and closing ones. */
	Collection<? extends RealityMarble> marbles(MinecraftServer server);

	/**
	 * Returns whether terrain is protected from breaking and placing, defaulting to all marble interiors.
	 * Sources with pending restoration must also include terrain that outlives its marble.
	 */
	default boolean protects(ServerLevel level, BlockPos pos) {
		Vec3 point = Vec3.atLowerCornerOf(pos);
		for (RealityMarble marble : this.marbles(level.getServer())) {
			if (marble.dimension().equals(level.dimension()) && marble.contains(point)) {
				return true;
			}
		}
		return false;
	}
}
