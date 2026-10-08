package io.github.cwzmorro.alayacore.ability;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * Defines a castable ability registered in {@link AlayaRegistries#ABILITY}.
 * Hooks run on the server; a release before the hold threshold counts as a tap.
 */
public abstract class Ability {
	public static final String UNKNOWN_RANK = "???";
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

	/** Returns whether this ability must be the entity's only active cast. */
	public boolean exclusive() {
		return this.exclusive;
	}

	public boolean noblePhantasm() {
		return this.noblePhantasm;
	}

	public List<Double> defaultCooldownSeconds() {
		return this.cooldownSeconds;
	}

	/**
	 * Returns the settings exposed in the preset screen, read through {@link AbilitySettings#value}.
	 * Called on each query so config-backed bounds can follow reloads; empty by default.
	 */
	public List<AbilitySetting> settings() {
		return List.of();
	}

	/** Returns the displayed rank or range, {@link #UNKNOWN_RANK} for an unknown rank, or null to hide it. */
	public @Nullable String rankLabel() {
		return null;
	}

	public static Component name(Identifier id) {
		return Component.translatable(id.toLanguageKey("ability"));
	}

	public static int modes(Identifier id) {
		return AlayaRegistries.ABILITY.getOptional(id).map(Ability::modes).orElse(1);
	}

	public static List<AbilitySetting> settings(Identifier id) {
		return AlayaRegistries.ABILITY.getOptional(id).map(Ability::settings).orElse(List.of());
	}

	public static Component modeName(Identifier id, int mode) {
		return Component.translatable(id.toLanguageKey("ability", "mode." + (mode + 1)));
	}

	/**
	 * Returns description lines from {@code ability.<namespace>.<path>.description.<n>} translations,
	 * starting at 1 and stopping at the first missing key.
	 */
	public static List<Component> description(Identifier id) {
		return lines(id, id.toLanguageKey("ability", "description"));
	}

	public static List<Component> modeDescription(Identifier id, int mode) {
		return lines(id, id.toLanguageKey("ability", "mode." + (mode + 1) + ".description"));
	}

	/**
	 * Returns translation arguments for description lines. Runs on the client; values must be available there.
	 */
	public List<Object> descriptionArguments() {
		return List.of();
	}

	private static List<Component> lines(Identifier id, String keyPrefix) {
		Language language = Language.getInstance();
		Object[] arguments = AlayaRegistries.ABILITY.getOptional(id).map(ability -> ability.descriptionArguments().toArray()).orElse(new Object[0]);
		List<Component> lines = new ArrayList<>();
		for (String key = keyPrefix + ".1"; language.has(key); key = keyPrefix + "." + (lines.size() + 1)) {
			lines.add(Component.translatable(key, arguments));
		}
		return lines;
	}

	/**
	 * Returns an action-bar refusal message, or null to allow the cast after built-in checks pass.
	 * Refusing starts no cooldown.
	 */
	public @Nullable Component cannotStart(LivingEntity entity, int mode) {
		return null;
	}

	public double startCost(LivingEntity entity, int mode) {
		return 0.0;
	}

	public void onStart(AbilityCast cast) {
	}

	/** Handles a press while this ability's cast is still active. */
	public void onPressedAgain(AbilityCast cast) {
	}

	public void onHoldStart(AbilityCast cast) {
	}

	public void onHeldTick(AbilityCast cast) {
	}

	/** Handles release before the hold threshold; ends the cast by default. */
	public void onTap(AbilityCast cast) {
		cast.end();
	}

	/** Handles release after the hold threshold; ends the cast by default. */
	public void onReleased(AbilityCast cast) {
		cast.end();
	}

	public void onTick(AbilityCast cast) {
	}

	/** Cleans up a cast after it ends, including interruption by death, logout or a servant change. */
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

		public Properties cooldownSeconds(double... perMode) {
			this.cooldownSeconds = Arrays.stream(perMode).boxed().toList();
			return this;
		}
	}
}
