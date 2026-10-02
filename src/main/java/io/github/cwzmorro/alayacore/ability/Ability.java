package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Something a servant can cast (e.g. Gate of Babylon). Content mods register theirs in
 * {@link AlayaRegistries#ABILITY} and list them on their servants.
 *
 * <p>A press starts a {@link AbilityCast}, which stays active until the ability ends it. A press
 * released before the hold threshold (plan 8) is a tap, otherwise a hold. The hooks below are called
 * on the server with the cast; every one does nothing by default except where noted.
 */
public abstract class Ability {
	private final int modes;
	private final boolean exclusive;
	private final boolean noblePhantasm;
	private final List<Double> cooldownSeconds;

	protected Ability(Properties properties) {
		this.modes = properties.modes;
		this.exclusive = properties.exclusive;
		this.noblePhantasm = properties.noblePhantasm;
		this.cooldownSeconds = properties.cooldownSeconds == null ? Collections.nCopies(this.modes, 0.0) : properties.cooldownSeconds;
		if (this.cooldownSeconds.size() != this.modes) {
			throw new IllegalArgumentException("expected " + this.modes + " cooldowns, one per mode, got " + this.cooldownSeconds.size());
		}
	}

	public int modes() {
		return this.modes;
	}

	/** While an exclusive ability is active nothing else can be activated, and it can't start while another is active (plan 8). */
	public boolean exclusive() {
		return this.exclusive;
	}

	/** A noble phantasm can't be used before true name realization (plan 13.5). */
	public boolean noblePhantasm() {
		return this.noblePhantasm;
	}

	/** Built-in cooldown of each mode, in seconds; the config can change them. */
	public List<Double> defaultCooldownSeconds() {
		return this.cooldownSeconds;
	}

	/** Translation key {@code ability.<namespace>.<path>}. */
	public static Component name(Identifier id) {
		return Component.translatable(id.toLanguageKey("ability"));
	}

	/** How many modes the registered ability has; 1 for an unknown id. */
	public static int modes(Identifier id) {
		return AlayaRegistries.ABILITY.getOptional(id).map(Ability::modes).orElse(1);
	}

	/** Translation key {@code ability.<namespace>.<path>.mode.<n>}, n counted from 1. */
	public static Component modeName(Identifier id, int mode) {
		return Component.translatable(id.toLanguageKey("ability", "mode." + (mode + 1)));
	}

	/**
	 * Why this entity can't start the ability right now (e.g. a weapon not in hand), shown to a player on the action bar;
	 * null if it can. Checked after the built-in checks; a refused press starts no cooldown.
	 */
	public @Nullable Component cannotStart(LivingEntity entity, int mode) {
		return null;
	}

	/** Mana needed to start, paid when the cast starts; if the entity has less, the press is refused. */
	public double startCost(LivingEntity entity, int mode) {
		return 0.0;
	}

	/** First press: the cast has just started. */
	public void onStart(AbilityCast cast) {
	}

	/** Pressed again while this ability's cast is still active (e.g. Gate of Babylon's second press). */
	public void onPressedAgain(AbilityCast cast) {
	}

	/** Held past the threshold: from now on this press is a hold. */
	public void onHoldStart(AbilityCast cast) {
	}

	/** Every tick while held past the threshold. */
	public void onHeldTick(AbilityCast cast) {
	}

	/** Released before the threshold. Ends the cast by default. */
	public void onTap(AbilityCast cast) {
		cast.end();
	}

	/** Released after the threshold. Ends the cast by default. */
	public void onReleased(AbilityCast cast) {
		cast.end();
	}

	/** Every tick while the cast is active, held or not. */
	public void onTick(AbilityCast cast) {
	}

	/** The cast has ended, by the ability, lack of mana, death, logout or a servant change. Clean up here. */
	public void onEnd(AbilityCast cast) {
	}

	public static final class Properties {
		private int modes = 1;
		private boolean exclusive;
		private boolean noblePhantasm;
		private @Nullable List<Double> cooldownSeconds;

		public Properties modes(int modes) {
			if (modes < 1) {
				throw new IllegalArgumentException("an ability needs at least one mode, got " + modes);
			}
			this.modes = modes;
			return this;
		}

		public Properties exclusive() {
			this.exclusive = true;
			return this;
		}

		public Properties noblePhantasm() {
			this.noblePhantasm = true;
			return this;
		}

		/** One value per mode. */
		public Properties cooldownSeconds(double... perMode) {
			this.cooldownSeconds = Arrays.stream(perMode).boxed().toList();
			return this;
		}
	}
}
