package io.github.cwzmorro.alayacore.servant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Where servant choices are kept (Fabric data attachments). */
public final class ServantStorage {
	private static final Codec<ServantChoice> CHOICE_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.BOOL.fieldOf("decided").forGetter(ServantChoice::decided),
		Identifier.CODEC.optionalFieldOf("servant").forGetter(ServantChoice::servant)
	).apply(i, ServantChoice::new));

	private static final StreamCodec<ByteBuf, ServantChoice> CHOICE_STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, ServantChoice::decided,
		ByteBufCodecs.optional(Identifier.STREAM_CODEC), ServantChoice::servant,
		ServantChoice::new
	);

	// The choice is saved, kept through death and sent only to its player; choosing and frozen-at are not saved.
	public static final AttachmentType<ServantChoice> CHOICE = AttachmentRegistry.create(AlayaCore.id("servant_choice"), builder -> builder
		.initializer(() -> ServantChoice.UNDECIDED)
		.persistent(CHOICE_CODEC)
		.copyOnDeath()
		.syncWith(CHOICE_STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	public static final AttachmentType<Boolean> CHOOSING = AttachmentRegistry.create(AlayaCore.id("choosing_servant"));

	public static final AttachmentType<Vec3> FROZEN_AT = AttachmentRegistry.create(AlayaCore.id("frozen_at"));

	// Server-wide: which player is which servant. Saved with the world and sent to every client, so the screen can show taken servants.
	public static final AttachmentType<Map<UUID, Identifier>> OWNERS = AttachmentRegistry.create(AlayaCore.id("servant_owners"), builder -> builder
		.initializer(Map::of)
		.persistent(Codec.unboundedMap(UUIDUtil.STRING_CODEC, Identifier.CODEC))
		.syncWith(ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, Identifier.STREAM_CODEC), AttachmentSyncPredicate.all()));

	private ServantStorage() {
	}

	public static void init() {
	}
}
