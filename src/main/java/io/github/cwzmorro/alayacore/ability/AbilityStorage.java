package io.github.cwzmorro.alayacore.ability;

import com.mojang.serialization.Codec;
import io.github.cwzmorro.alayacore.AlayaCore;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/** Where ability state is kept on an entity (Fabric data attachments). */
public final class AbilityStorage {
	/** Casts in progress, by ability. Server only, not saved; the system ends them on death, logout and unload. */
	static final AttachmentType<Map<Identifier, AbilityCast>> CASTS = AttachmentRegistry.create(AlayaCore.id("ability_casts"),
		builder -> builder.initializer(LinkedHashMap::new));

	/** Per ability, the game tick each mode's cooldown ends. Saved, kept through death, sent to its owner for the HUD. */
	public static final AttachmentType<Map<Identifier, List<Long>>> COOLDOWNS = AttachmentRegistry.create(AlayaCore.id("ability_cooldowns"),
		builder -> builder
			.initializer(Map::of)
			.persistent(Codec.unboundedMap(Identifier.CODEC, Codec.LONG.listOf()))
			.copyOnDeath()
			.syncWith(ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list())),
				AttachmentSyncPredicate.targetOnly()));

	/** A player's presets (plan 8). Saved, kept through death, sent to its owner. */
	public static final AttachmentType<AbilityPresets> PRESETS = AttachmentRegistry.create(AlayaCore.id("ability_presets"),
		builder -> builder
			.initializer(() -> AbilityPresets.EMPTY)
			.persistent(AbilityPresets.CODEC)
			.copyOnDeath()
			.syncWith(AbilityPresets.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private AbilityStorage() {
	}

	/** Loads this class, which registers the attachments. */
	public static void init() {
	}
}
