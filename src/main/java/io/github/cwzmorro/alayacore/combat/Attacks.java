package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** The {@link AttackData} on attack entities (a Fabric data attachment, saved with the entity). */
public final class Attacks {
	public static final AttachmentType<AttackData> ATTACK = AttachmentRegistry.create(AlayaCore.id("attack"), builder -> builder
		.persistent(AttackData.CODEC));

	private Attacks() {
	}

	/** The attack data of {@code entity}, or null for an entity that isn't a ranked attack (or for no entity). */
	public static @Nullable AttackData get(@Nullable Entity entity) {
		return entity == null ? null : entity.getAttached(ATTACK);
	}

	/** Loads this class, which registers the attachment. */
	public static void init() {
	}
}
