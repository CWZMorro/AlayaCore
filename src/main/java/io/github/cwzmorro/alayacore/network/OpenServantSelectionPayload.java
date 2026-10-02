package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client: open the servant selection screen.
 *
 * @param forceRandom     only "become a servant (random)" or "human" are offered
 * @param allowDuplicates taken servants may still be chosen
 * @param closable        Esc closes it without a change (the Spirit Origin Changer); false on first join
 */
public record OpenServantSelectionPayload(boolean forceRandom, boolean allowDuplicates, boolean closable) implements CustomPacketPayload {
	public static final Type<OpenServantSelectionPayload> TYPE = new Type<>(AlayaCore.id("open_servant_selection"));
	public static final StreamCodec<ByteBuf, OpenServantSelectionPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, OpenServantSelectionPayload::forceRandom,
		ByteBufCodecs.BOOL, OpenServantSelectionPayload::allowDuplicates,
		ByteBufCodecs.BOOL, OpenServantSelectionPayload::closable,
		OpenServantSelectionPayload::new
	);

	@Override
	public Type<OpenServantSelectionPayload> type() {
		return TYPE;
	}
}
