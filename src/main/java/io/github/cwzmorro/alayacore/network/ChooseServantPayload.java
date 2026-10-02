package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: the answer on the selection screen. The server checks it before applying it.
 *
 * @param servant the servant, only for {@link Kind#SERVANT}
 */
public record ChooseServantPayload(Kind kind, Optional<Identifier> servant) implements CustomPacketPayload {
	public enum Kind {
		SERVANT,
		RANDOM,
		HUMAN,
		/** Closed without choosing; only allowed once the player has decided before (plan 3). */
		CANCEL
	}

	public static final Type<ChooseServantPayload> TYPE = new Type<>(AlayaCore.id("choose_servant"));
	public static final StreamCodec<ByteBuf, ChooseServantPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal), ChooseServantPayload::kind,
		ByteBufCodecs.optional(Identifier.STREAM_CODEC), ChooseServantPayload::servant,
		ChooseServantPayload::new
	);

	@Override
	public Type<ChooseServantPayload> type() {
		return TYPE;
	}
}
