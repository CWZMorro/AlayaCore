package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: move a preset slot to its ability's next or previous mode (plan 8).
 *
 * @param delta +1 for the next mode, -1 for the previous one
 */
public record ShiftSlotModePayload(int preset, int slot, int delta) implements CustomPacketPayload {
	public static final Type<ShiftSlotModePayload> TYPE = new Type<>(AlayaCore.id("shift_slot_mode"));
	public static final StreamCodec<ByteBuf, ShiftSlotModePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ShiftSlotModePayload::preset,
		ByteBufCodecs.VAR_INT, ShiftSlotModePayload::slot,
		ByteBufCodecs.VAR_INT, ShiftSlotModePayload::delta,
		ShiftSlotModePayload::new
	);

	@Override
	public Type<ShiftSlotModePayload> type() {
		return TYPE;
	}
}
