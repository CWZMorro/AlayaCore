package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: make this preset the active one (plan 8). */
public record SelectPresetPayload(int preset) implements CustomPacketPayload {
	public static final Type<SelectPresetPayload> TYPE = new Type<>(AlayaCore.id("select_preset"));
	public static final StreamCodec<ByteBuf, SelectPresetPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SelectPresetPayload::preset,
		SelectPresetPayload::new
	);

	@Override
	public Type<SelectPresetPayload> type() {
		return TYPE;
	}
}
