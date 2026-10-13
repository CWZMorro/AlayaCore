package io.github.cwzmorro.alayacore.combat;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Deals continuous damage through hit cooldowns without knockback, calculating armor reduction per second.
 * Carries attack data on its direct entity ({@link Attacks#of}).
 */
public final class ContinuousSource extends DamageSource {
	public ContinuousSource(Holder<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
		super(type, direct, causing);
	}

	public static ContinuousSource of(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
		return new ContinuousSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), direct, causing);
	}

	public static float perTick(double perSecond) {
		return (float) (perSecond / SharedConstants.TICKS_PER_SECOND);
	}

	/** Returns the per-second armor basis for continuous damage, or the hit amount otherwise. */
	public static float armorBasis(DamageSource source, float damage) {
		return source instanceof ContinuousSource ? damage * SharedConstants.TICKS_PER_SECOND : damage;
	}

	@Override
	public boolean is(TagKey<DamageType> tag) {
		return DamageTypeTags.NO_KNOCKBACK.equals(tag) || super.is(tag);
	}
}
