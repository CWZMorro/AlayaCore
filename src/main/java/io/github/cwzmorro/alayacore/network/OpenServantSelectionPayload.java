package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: open the servant selection screen.
 *
 * @param forceRandom     only "become a servant (random)" or "human" are offered
 * @param allowDuplicates taken servants may still be chosen
 * @param closable        Esc closes it without a change (the Spirit Origin Changer); false on first join
 * @param demi            the demi stats every class starts from, from the server's config
 * @param realStats       every class's real stats, from the server's config, by class id
 */
public record OpenServantSelectionPayload(boolean forceRandom, boolean allowDuplicates, boolean closable, ServantStats demi,
	Map<Identifier, ServantStats> realStats) implements CustomPacketPayload {
	public static final Type<OpenServantSelectionPayload> TYPE = new Type<>(AlayaCore.id("open_servant_selection"));
	public static final StreamCodec<ByteBuf, OpenServantSelectionPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, OpenServantSelectionPayload::forceRandom,
		ByteBufCodecs.BOOL, OpenServantSelectionPayload::allowDuplicates,
		ByteBufCodecs.BOOL, OpenServantSelectionPayload::closable,
		ServantStats.STREAM_CODEC, OpenServantSelectionPayload::demi,
		ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ServantStats.STREAM_CODEC), OpenServantSelectionPayload::realStats,
		OpenServantSelectionPayload::new
	);

	@Override
	public Type<OpenServantSelectionPayload> type() {
		return TYPE;
	}
}
