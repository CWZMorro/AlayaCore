package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.mana.ManaData;
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

	/** Game tick of the first press. */
	public long startTick() {
		return this.startTick;
	}

	/** Whether the entity was sneaking at the latest press (plan 4: Shift + ability). */
	public boolean sneaking() {
		return this.sneaking;
	}

	/** Whether the ability's key is down right now. */
	public boolean held() {
		return this.held;
	}

	/** Ticks the key has been down since the latest press. */
	public int heldTicks() {
		return this.heldTicks;
	}

	/** Whether the latest press passed the hold threshold. */
	public boolean holding() {
		return this.holding;
	}

	public boolean ended() {
		return this.ended;
	}

	/**
	 * Pays mana for what the ability is about to do. With too little, nothing is paid, the entity is
	 * told, and the cast ends (plan 8).
	 *
	 * @return whether it was paid
	 */
	public boolean pay(double mana) {
		ManaData current = AlayaMana.get(this.entity);
		if (current == null || !AbilityRules.canAfford(current.current(), mana)) {
			Abilities.refuse(this.entity, AbilityRules.Refusal.NOT_ENOUGH_MANA, 0.0);
			this.end();
			return false;
		}
		AlayaMana.addCurrent(this.entity, -mana);
		return true;
	}

	/** Ends the cast; {@link Ability#onEnd} runs once the current hook returns. */
	public void end() {
		this.ended = true;
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

	/** Counts one more tick of holding; true when this tick crosses the hold threshold. */
	boolean tickHeld(int thresholdTicks) {
		this.heldTicks++;
		if (!this.holding && AbilityRules.isHold(this.heldTicks, thresholdTicks)) {
			this.holding = true;
			return true;
		}
		return false;
	}
}
