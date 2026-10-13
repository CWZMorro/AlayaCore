package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.rank.RankLetter;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Applies configured armor and Protection bypass from servant status, attack rank and damage tags.
 * Uses the largest applicable bypass rather than adding them.
 */
public final class AttackDefence {
	/** Damage types whose hits ignore defence as {@link CombatConfig#ignoresDefenceArmor} and {@link CombatConfig#ignoresDefenceProtection} say; a content mod adds its own. */
	public static final TagKey<DamageType> IGNORES_DEFENCE = TagKey.create(Registries.DAMAGE_TYPE, AlayaCore.id("ignores_defence"));
	// Vanilla's limit on enchantment protection.
	private static final float MOST_PROTECTION = 20.0F;

	private AttackDefence() {
	}

	public static float armor(DamageSource source, float armor) {
		return (float) (armor * (1.0 - armorIgnoredByRank(source)));
	}

	/**
	 * Returns damage after armor, restoring the configured share of blocked damage for {@link #IGNORES_DEFENCE} types.
	 */
	public static float damageAfterArmor(DamageSource source, float damage, float afterArmor) {
		if (!source.is(IGNORES_DEFENCE)) {
			return afterArmor;
		}
		return (float) (damage - (1.0 - AlayaConfig.combat().ignoresDefenceArmor()) * (damage - afterArmor));
	}

	public static float protection(DamageSource source, float protection) {
		boolean ignoresDefence = source.is(IGNORES_DEFENCE);
		double ignored = Math.max(servantIgnored(source), Math.max(protectionIgnoredByRank(source), ignoresDefence ? AlayaConfig.combat().ignoresDefenceProtection() : 0.0));
		return (float) ((ignoresDefence ? Math.min(MOST_PROTECTION, protection) : protection) * (1.0 - ignored));
	}

	public static boolean wearsArmor(DamageSource source) {
		return !source.is(IGNORES_DEFENCE) && armorIgnoredByRank(source) < 1.0;
	}

	private static double servantIgnored(DamageSource source) {
		return source.getEntity() instanceof LivingEntity attacker && AlayaServants.servantId(attacker).isPresent()
			? AlayaConfig.combat().servantProtectionIgnored() : 0.0;
	}

	private static double armorIgnoredByRank(DamageSource source) {
		return ignoredByRank(source, AlayaConfig.combat()::npArmorIgnored);
	}

	private static double protectionIgnoredByRank(DamageSource source) {
		return ignoredByRank(source, AlayaConfig.combat()::npProtectionIgnored);
	}

	private static double ignoredByRank(DamageSource source, ToDoubleFunction<RankLetter> part) {
		RankLetter tier = tier(Attacks.of(source));
		return tier == null ? 0.0 : part.applyAsDouble(tier);
	}

	private static @Nullable RankLetter tier(@Nullable AttackData attack) {
		if (attack == null) {
			return null;
		}
		RankLetter letter = attack.rank().letter();
		if (attack.noblePhantasm()) {
			return letter;
		}
		return letter.ordinal() == 0 ? null : RankLetter.values()[letter.ordinal() - 1];
	}
}
