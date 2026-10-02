package io.github.cwzmorro.alayacore.servant;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Opens the servant selection screen again (plan 3). Creative-only: it has no recipe or loot, but
 * whoever holds it can use it. The old servant is freed once the new answer is applied.
 */
public final class SpiritOriginChangerItem extends Item {
	public SpiritOriginChangerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			ServantSelection.open(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}
}
