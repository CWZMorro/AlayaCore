package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.AlayaCore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Deals continuous damage through hit cooldowns without knockback, calculating armor reduction and wear per second.
 * Carries attack data on its direct entity ({@link Attacks#of}).
 */
public final class ContinuousSource extends DamageSource {
	// Per target, the game time each continuous attack last wore its armor; never saved.
	private static final AttachmentType<Map<UUID, Long>> ARMOR_WORN = AttachmentRegistry.<Map<UUID, Long>>create(
		AlayaCore.id("continuous_armor_worn"), builder -> builder.initializer(HashMap::new));
	private static final UUID NO_ATTACKER = new UUID(0L, 0L);

	public ContinuousSource(Holder<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
		super(type, direct, causing);
	}

	public static ContinuousSource of(Level level, ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
		return new ContinuousSource(Attacks.damageType(level, type), direct, causing);
	}

	public static float perTick(double perSecond) {
		return (float) (perSecond / SharedConstants.TICKS_PER_SECOND);
	}

	/** Returns the per-second armor basis for continuous damage, or the hit amount otherwise. */
	public static float armorBasis(DamageSource source, float damage) {
		return source instanceof ContinuousSource ? damage * SharedConstants.TICKS_PER_SECOND : damage;
	}

	/**
	 * Returns armor wear and records the attack's wear tick on the victim.
	 * Continuous attacks wear armor once per second using per-second damage; ordinary hits use their full damage.
	 */
	public static float armorWear(LivingEntity victim, DamageSource source, float damage) {
		if (!(source instanceof ContinuousSource)) {
			return damage;
		}
		Entity attacker = Attacks.dealer(source);
		UUID attack = attacker == null ? NO_ATTACKER : attacker.getUUID();
		long now = victim.level().getGameTime();
		Map<UUID, Long> worn = victim.getAttachedOrCreate(ARMOR_WORN);
		worn.values().removeIf(time -> now - time >= SharedConstants.TICKS_PER_SECOND);
		if (worn.putIfAbsent(attack, now) != null) {
			return 0.0F;
		}
		return armorBasis(source, damage);
	}

	public static void init() {
	}

	@Override
	public boolean is(TagKey<DamageType> tag) {
		return DamageTypeTags.NO_KNOCKBACK.equals(tag) || super.is(tag);
	}
}
