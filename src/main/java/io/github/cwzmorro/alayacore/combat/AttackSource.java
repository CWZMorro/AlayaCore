package io.github.cwzmorro.alayacore.combat;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * The damage source of an attack that has no entity of its own to carry its {@link AttackData} (an ability that hurts
 * what is around its caster, say); {@link Attacks#of} finds the data on it.
 */
public final class AttackSource extends DamageSource {
	private final AttackData attack;

	public AttackSource(Holder<DamageType> type, @Nullable Entity direct, @Nullable Entity causing, AttackData attack) {
		super(type, direct, causing);
		this.attack = attack;
	}

	public AttackData attack() {
		return this.attack;
	}
}
