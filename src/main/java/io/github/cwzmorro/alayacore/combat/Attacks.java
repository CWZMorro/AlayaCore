package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** The {@link AttackData} on attack entities (a Fabric data attachment, saved with the entity). */
public final class Attacks {
	public static final AttachmentType<AttackData> ATTACK = AttachmentRegistry.create(AlayaCore.id("attack"), builder -> builder
		.persistent(AttackData.CODEC));

	private Attacks() {
	}

	public static @Nullable AttackData get(@Nullable Entity entity) {
		return entity == null ? null : entity.getAttached(ATTACK);
	}

	/** Returns data from an {@link AttackSource} or its direct entity, or null if neither carries data. */
	public static @Nullable AttackData of(DamageSource source) {
		return source instanceof AttackSource attackSource ? attackSource.attack() : get(source.getDirectEntity());
	}

	public static void init() {
	}
}
