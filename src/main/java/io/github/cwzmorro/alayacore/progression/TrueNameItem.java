package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.api.AlayaServants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Instantly realizes only the current servant's true name; consumed except in creative. */
public final class TrueNameItem extends Item {
	private static final int CONSUMED_PER_USE = 1;

	public TrueNameItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		if (AlayaServants.servantId(serverPlayer).isEmpty()) {
			return refuse(serverPlayer, "not_a_servant");
		}
		if (TrueName.isRealized(serverPlayer)) {
			return refuse(serverPlayer, "already_realized");
		}
		TrueName.realize(serverPlayer);
		if (!TrueName.isRealized(serverPlayer)) {
			return refuse(serverPlayer, "unavailable");
		}
		player.getItemInHand(hand).consume(CONSUMED_PER_USE, player);
		serverPlayer.sendOverlayMessage(Component.translatable("item.alayacore.true_name_realization.realized"));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult refuse(ServerPlayer player, String reason) {
		player.sendOverlayMessage(Component.translatable("item.alayacore.true_name_realization." + reason));
		return InteractionResult.FAIL;
	}
}
