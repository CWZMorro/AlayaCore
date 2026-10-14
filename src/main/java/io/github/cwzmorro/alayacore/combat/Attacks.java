package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.AlayaCore;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The {@link AttackData} on attack entities (a Fabric data attachment, saved with the entity), and the lookups every attack's
 * damage source needs.
 */
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

	public static @Nullable Entity dealer(DamageSource source) {
		return source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
	}

	public static Holder<DamageType> damageType(Level level, ResourceKey<DamageType> key) {
		return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
	}

	public static void init() {
	}
}
