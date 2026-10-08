package io.github.cwzmorro.alayacore.network;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Server → players tracking a shield: a hit took HP off it, so content mods can draw it.
 *
 * @param shieldId the shield's entity id
 * @param point    where the hit landed on it, in world coordinates
 * @param hpTaken  the HP it took, a rank break's too
 */
public record ShieldHitPayload(int shieldId, Vec3 point, float hpTaken) implements CustomPacketPayload {
	public static final Type<ShieldHitPayload> TYPE = new Type<>(AlayaCore.id("shield_hit"));
	public static final StreamCodec<ByteBuf, ShieldHitPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ShieldHitPayload::shieldId,
		Vec3.STREAM_CODEC, ShieldHitPayload::point,
		ByteBufCodecs.FLOAT, ShieldHitPayload::hpTaken,
		ShieldHitPayload::new
	);

	@Override
	public Type<ShieldHitPayload> type() {
		return TYPE;
	}
}
