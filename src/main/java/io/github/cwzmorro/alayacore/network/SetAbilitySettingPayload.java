package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client → server: choose this value for an ability's setting; the server clamps it to the setting's range. */
public record SetAbilitySettingPayload(Identifier ability, String setting, double value) implements CustomPacketPayload {
	public static final Type<SetAbilitySettingPayload> TYPE = new Type<>(AlayaCore.id("set_ability_setting"));
	public static final StreamCodec<ByteBuf, SetAbilitySettingPayload> STREAM_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, SetAbilitySettingPayload::ability,
		ByteBufCodecs.STRING_UTF8, SetAbilitySettingPayload::setting,
		ByteBufCodecs.DOUBLE, SetAbilitySettingPayload::value,
		SetAbilitySettingPayload::new
	);

	@Override
	public Type<SetAbilitySettingPayload> type() {
		return TYPE;
	}
}
