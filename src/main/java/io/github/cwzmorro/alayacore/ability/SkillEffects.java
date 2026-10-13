package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.mixin.MobEffectInstanceAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Applies reversible skill effects and attribute boosts without removing unrelated potion effects.
 */
public final class SkillEffects {
	// Vanilla's Regeneration heals every this many ticks at level I, twice as often each level up (the literal
	// 50 >> amplification in RegenerationMobEffect.shouldApplyEffectTickThisTick).
	public static final int REGENERATION_PERIOD_TICKS = 50;

	private record Applied(long start, int amplifier, int duration, long priorAt, List<MobEffectInstance> prior) { }

	private static final AttachmentType<Map<String, Applied>> APPLIED = AttachmentRegistry.create(AlayaCore.id("skill_effects"));
	// The effect's countdown and the game clock are read at different points of a tick, so they may differ by this much.
	private static final int CLOCK_TOLERANCE_TICKS = 1;

	private SkillEffects() { }

	/** Applies {@code effect} to {@code entity} for {@code skill}; call again to refresh it (the first call's prior chain is kept). */
	public static void give(LivingEntity entity, Identifier skill, MobEffectInstance effect) {
		Map<String, Applied> all = new HashMap<>(entity.getAttachedOrElse(APPLIED, Map.of()));
		String key = key(skill, effect.getEffect());
		long now = entity.level().getGameTime();
		Applied before = all.get(key);
		List<MobEffectInstance> prior = before == null ? chain(entity, effect.getEffect()) : before.prior();
		all.put(key, new Applied(now, effect.getAmplifier(), effect.getDuration(), before == null ? now : before.priorAt(), prior));
		entity.setAttached(APPLIED, Map.copyOf(all));
		entity.addEffect(effect);
	}

	/**
	 * Removes the skill's effect and restores displaced effects that have not expired.
	 * Leaves stronger or later potions untouched; does nothing if the skill applied no effect.
	 */
	public static void take(LivingEntity entity, Identifier skill, Holder<MobEffect> effect) {
		Map<String, Applied> all = new HashMap<>(entity.getAttachedOrElse(APPLIED, Map.of()));
		Applied applied = all.remove(key(skill, effect));
		if (applied == null) {
			return;
		}
		if (all.isEmpty()) {
			entity.removeAttached(APPLIED);
		} else {
			entity.setAttached(APPLIED, Map.copyOf(all));
		}
		if (!entity.isAlive()) {
			return;
		}
		long now = entity.level().getGameTime();
		long ours = applied.start() + applied.duration() - now;
		List<MobEffectInstance> chain = chain(entity, effect);
		boolean changed = false;
		for (int i = 0; i < chain.size() && !changed; i++) {
			MobEffectInstance node = chain.get(i);
			if (node.getAmplifier() == applied.amplifier() && Math.abs(node.getDuration() - Math.max(ours, 0)) <= CLOCK_TOLERANCE_TICKS) {
				chain.remove(i);
				changed = true;
			}
		}
		for (MobEffectInstance node : applied.prior()) {
			long left = node.getDuration() - (now - applied.priorAt());
			if (left > 0 && chain.stream().noneMatch(kept -> kept.getAmplifier() == node.getAmplifier() && kept.getDuration() >= left - CLOCK_TOLERANCE_TICKS)) {
				chain.add(plain(node, (int) left));
				changed = true;
			}
		}
		if (!changed) {
			return;
		}
		// Weakest first: adding the stronger ones after puts the weaker, longer ones back beneath them, as vanilla does.
		entity.removeEffect(effect);
		chain.sort(Comparator.comparingInt(MobEffectInstance::getAmplifier));
		for (MobEffectInstance node : chain) {
			entity.addEffect(node);
		}
	}

	public static void boost(LivingEntity entity, Holder<Attribute> attribute, Identifier id, double bonus) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	public static void unboost(LivingEntity entity, Holder<Attribute> attribute, Identifier id) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}

	private static String key(Identifier skill, Holder<MobEffect> effect) {
		return skill + "/" + effect.getRegisteredName();
	}

	private static List<MobEffectInstance> chain(LivingEntity entity, Holder<MobEffect> effect) {
		List<MobEffectInstance> chain = new ArrayList<>();
		for (MobEffectInstance node = entity.getEffect(effect); node != null; node = ((MobEffectInstanceAccessor) node).alayacore$hiddenEffect()) {
			chain.add(plain(node, node.getDuration()));
		}
		return chain;
	}

	private static MobEffectInstance plain(MobEffectInstance node, int duration) {
		return new MobEffectInstance(node.getEffect(), duration, node.getAmplifier(), node.isAmbient(), node.isVisible(), node.showIcon());
	}
}
