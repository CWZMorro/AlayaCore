package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * One use of an ability by one entity, from the first press until the ability (or the system) ends it.
 * Server side only; not saved.
 */
public final class AbilityCast {
	private final LivingEntity entity;
	private final Identifier abilityId;
	private final Ability ability;
	private final int mode;
	private final long startTick;
	private boolean sneaking;
	private boolean held = true;
	private int heldTicks;
	private boolean holding;
	private boolean ended;

	AbilityCast(LivingEntity entity, Identifier abilityId, Ability ability, int mode, boolean sneaking, long startTick) {
		this.entity = entity;
		this.abilityId = abilityId;
		this.ability = ability;
		this.mode = mode;
		this.sneaking = sneaking;
		this.startTick = startTick;
	}

	public LivingEntity entity() {
		return this.entity;
	}

	public Identifier abilityId() {
		return this.abilityId;
	}

	public Ability ability() {
		return this.ability;
	}

	public int mode() {
		return this.mode;
	}

	public long startTick() {
		return this.startTick;
	}

	public boolean sneaking() {
		return this.sneaking;
	}

	public boolean held() {
		return this.held;
	}

	public int heldTicks() {
		return this.heldTicks;
	}

	public boolean holding() {
		return this.holding;
	}

	public boolean ended() {
		return this.ended;
	}

	/**
	 * Pays mana, or ends the cast with a refusal message if the entity cannot afford it.
	 * An unaffordable payment spends nothing.
	 *
	 * @return whether it was paid
	 */
	public boolean pay(double mana) {
		if (!AbilityRules.canAfford(AlayaMana.available(this.entity), mana)) {
			this.endOutOfMana();
			return false;
		}
		AlayaMana.spend(this.entity, mana);
		return true;
	}

	/** Ends the cast; {@link Ability#onEnd} runs once the current hook returns. */
	public void end() {
		this.ended = true;
	}

	public void endOutOfMana() {
		Abilities.refuse(this.entity, AbilityRules.Refusal.NOT_ENOUGH_MANA, 0.0);
		this.end();
	}

	void press(boolean sneaking) {
		this.sneaking = sneaking;
		this.held = true;
		this.heldTicks = 0;
		this.holding = false;
	}

	void release() {
		this.held = false;
	}

	boolean tickHeld(int thresholdTicks) {
		this.heldTicks++;
		if (!this.holding && AbilityRules.isHold(this.heldTicks, thresholdTicks)) {
			this.holding = true;
			return true;
		}
		return false;
	}
}
