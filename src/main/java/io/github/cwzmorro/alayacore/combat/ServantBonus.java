package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.progression.Progression;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The servant's own bonus on the damage of an attack: its current class attack, which grows with sync. A skill or
 * noble phantasm adds {@link #classAttack} per hit; a held, pulsed or continuous attack adds it once a second, split across
 * that second's hits; a melee hit adds {@link #melee}. A human, or any entity with no servant, adds nothing.
 */
public final class ServantBonus {
	private ServantBonus() {
	}

	public static double classAttack(LivingEntity entity) {
		return AlayaServants.classConfig(entity).map(config -> Progression.stats(entity, config).attack()).orElse(0.0);
	}

	/**
	 * Returns the class attack bonus scaled inversely by melee attack speed.
	 * Entities without an attack-speed attribute receive the full class attack.
	 */
	public static double melee(LivingEntity attacker) {
		double classAttack = classAttack(attacker);
		AttributeInstance speed = attacker.getAttribute(Attributes.ATTACK_SPEED);
		if (speed == null || speed.getValue() <= 0.0) {
			return classAttack;
		}
		return classAttack * AlayaConfig.combat().meleeReferenceAttacksPerSecond() / speed.getValue();
	}
}
