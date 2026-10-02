package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: an ability's key went down or up. The server checks it before using it.
 *
 * @param mode     the mode to start in; ignored when the key goes up
 * @param sneaking whether the player sneaks as the key goes down (plan 4: Shift + ability)
 */
public record AbilityKeyPayload(Identifier ability, int mode, boolean pressed, boolean sneaking) implements CustomPacketPayload {
	public static final Type<AbilityKeyPayload> TYPE = new Type<>(AlayaCore.id("ability_key"));
	public static final StreamCodec<ByteBuf, AbilityKeyPayload> STREAM_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, AbilityKeyPayload::ability,
		ByteBufCodecs.VAR_INT, AbilityKeyPayload::mode,
		ByteBufCodecs.BOOL, AbilityKeyPayload::pressed,
		ByteBufCodecs.BOOL, AbilityKeyPayload::sneaking,
		AbilityKeyPayload::new
	);

	@Override
	public Type<AbilityKeyPayload> type() {
		return TYPE;
	}
}
