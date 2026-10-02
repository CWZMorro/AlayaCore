package io.github.cwzmorro.alayacore.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Where mana is kept on an entity (Fabric data attachments). */
public final class ManaStorage {
	private static final Codec<ManaData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.DOUBLE.fieldOf("current").forGetter(ManaData::current),
		Codec.DOUBLE.fieldOf("max").forGetter(ManaData::max)
	).apply(i, ManaData::new));

	private static final StreamCodec<ByteBuf, ManaData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.DOUBLE, ManaData::current,
		ByteBufCodecs.DOUBLE, ManaData::max,
		ManaData::new
	);

	/** Saved, kept through death (respawn then sets current to half), sent only to its owner. */
	public static final AttachmentType<ManaData> MANA = AttachmentRegistry.create(AlayaCore.id("mana"), builder -> builder
		.initializer(() -> AlayaConfig.mana().start())
		.persistent(CODEC)
		.copyOnDeath()
		.syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	/** Challenge advancements that already paid their max-mana bonus to this player. Server only. */
	public static final AttachmentType<Set<Identifier>> PAID_ADVANCEMENTS = AttachmentRegistry.create(
		AlayaCore.id("paid_advancements"), builder -> builder
			.initializer(Set::of)
			.persistent(Identifier.CODEC.listOf().xmap(Set::copyOf, List::copyOf))
			.copyOnDeath());

	private ManaStorage() {
	}

	/** Loads this class, which registers the attachments. */
	public static void init() {
	}
}
