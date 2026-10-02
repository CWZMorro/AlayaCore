package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → server: put an ability in a preset slot, or empty the slot (plan 8).
 *
 * @param ability empty to clear the slot
 */
public record SetPresetSlotPayload(int preset, int slot, Optional<Identifier> ability) implements CustomPacketPayload {
	public static final Type<SetPresetSlotPayload> TYPE = new Type<>(AlayaCore.id("set_preset_slot"));
	public static final StreamCodec<ByteBuf, SetPresetSlotPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SetPresetSlotPayload::preset,
		ByteBufCodecs.VAR_INT, SetPresetSlotPayload::slot,
		ByteBufCodecs.optional(Identifier.STREAM_CODEC), SetPresetSlotPayload::ability,
		SetPresetSlotPayload::new
	);

	@Override
	public Type<SetPresetSlotPayload> type() {
		return TYPE;
	}
}
