package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.shield.ShieldConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, on joining and after every config reload: the server's {@code [shields]} values, so the client's ability
 * descriptions can show them.
 *
 * @param shields the server's shield settings
 */
public record ShieldConfigPayload(ShieldConfig shields) implements CustomPacketPayload {
	public static final Type<ShieldConfigPayload> TYPE = new Type<>(AlayaCore.id("shield_config"));
	public static final StreamCodec<ByteBuf, ShieldConfigPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, payload -> payload.shields().tapSeconds(),
		ByteBufCodecs.DOUBLE, payload -> payload.shields().holdUpkeepPerSecond(),
		ByteBufCodecs.DOUBLE, payload -> payload.shields().projectileReach(),
		(tapSeconds, holdUpkeepPerSecond, projectileReach) -> new ShieldConfigPayload(new ShieldConfig(tapSeconds, holdUpkeepPerSecond,
			projectileReach))
	);

	@Override
	public Type<ShieldConfigPayload> type() {
		return TYPE;
	}
}
