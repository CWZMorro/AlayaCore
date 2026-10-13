package io.github.cwzmorro.alayacore.ability;

/**
 * A {@link Ability#passive() passive} ability. It runs nothing itself: the content mod that registers it applies
 * its effect to every entity that {@link Abilities#owns owns} it.
 */
public class PassiveAbility extends Ability {
	public PassiveAbility() {
		super(new Properties().passive());
	}
}
