package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityCast;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Abilities used only by the game tests. Recorders note every hook they get, per entity, as
 * {@code "<ability>:<hook>"}; a tap keeps them active (like Gate of Babylon's open gates), and a
 * second press or a release after holding ends them.
 */
final class TestAbilities {
	static final ResourceKey<Ability> RECORDER = key("recorder");
	static final ResourceKey<Ability> EXCLUSIVE = key("exclusive");
	static final ResourceKey<Ability> NOBLE_PHANTASM = key("noble_phantasm");
	static final ResourceKey<Ability> COOLDOWN = key("cooldown");
	/** Costs {@link #START_COST} to start and {@link #HELD_COST} every tick while held. */
	static final ResourceKey<Ability> COSTLY = key("costly");
	/** Won't start unless a stick is in the main hand ({@link Ability#cannotStart}); has a cooldown. */
	static final ResourceKey<Ability> PICKY = key("picky");

	static final double COOLDOWN_SECONDS = 1.0;
	static final double START_COST = 500.0;
	static final double HELD_COST = 100.0;
	static final int RECORDER_MODES = 2;

	private static final Map<UUID, List<String>> EVENTS = new ConcurrentHashMap<>();

	private TestAbilities() {
	}

	private static ResourceKey<Ability> key(String path) {
		return ResourceKey.create(AlayaRegistries.ABILITY_KEY, Identifier.fromNamespaceAndPath("alayacore-gametest", path));
	}

	static void register() {
		Registry.register(AlayaRegistries.ABILITY, RECORDER, new Recorder(new Ability.Properties().modes(RECORDER_MODES)));
		Registry.register(AlayaRegistries.ABILITY, EXCLUSIVE, new Recorder(new Ability.Properties().exclusive()));
		Registry.register(AlayaRegistries.ABILITY, NOBLE_PHANTASM, new Recorder(new Ability.Properties().noblePhantasm()));
		Registry.register(AlayaRegistries.ABILITY, COOLDOWN, new Recorder(new Ability.Properties().cooldownSeconds(COOLDOWN_SECONDS)));
		Registry.register(AlayaRegistries.ABILITY, COSTLY, new Costly());
		Registry.register(AlayaRegistries.ABILITY, PICKY, new Picky());
	}

	/** Every hook this entity's recorders got, in order. */
	static List<String> events(LivingEntity entity) {
		return List.copyOf(EVENTS.getOrDefault(entity.getUUID(), List.of()));
	}

	private static final class Picky extends Recorder {
		private Picky() {
			super(new Ability.Properties().cooldownSeconds(COOLDOWN_SECONDS));
		}

		@Override
		public @Nullable Component cannotStart(LivingEntity entity, int mode) {
			return entity.getMainHandItem().is(Items.STICK) ? null : Component.literal("needs a stick");
		}
	}

	static String event(ResourceKey<Ability> ability, String hook) {
		return ability.identifier().getPath() + ":" + hook;
	}

	private static class Recorder extends Ability {
		Recorder(Properties properties) {
			super(properties);
		}

		private static void record(AbilityCast cast, String hook) {
			EVENTS.computeIfAbsent(cast.entity().getUUID(), uuid -> Collections.synchronizedList(new ArrayList<>()))
				.add(cast.abilityId().getPath() + ":" + hook);
		}

		@Override
		public void onStart(AbilityCast cast) {
			record(cast, "start");
		}

		@Override
		public void onPressedAgain(AbilityCast cast) {
			record(cast, "again");
			cast.end();
		}

		@Override
		public void onHoldStart(AbilityCast cast) {
			record(cast, "hold_start");
		}

		@Override
		public void onHeldTick(AbilityCast cast) {
			record(cast, "held");
		}

		@Override
		public void onTap(AbilityCast cast) {
			record(cast, "tap");
		}

		@Override
		public void onReleased(AbilityCast cast) {
			record(cast, "released");
			cast.end();
		}

		@Override
		public void onEnd(AbilityCast cast) {
			record(cast, "end");
		}
	}

	private static final class Costly extends Ability {
		private Costly() {
			super(new Properties());
		}

		@Override
		public double startCost(LivingEntity entity, int mode) {
			return START_COST;
		}

		@Override
		public void onHeldTick(AbilityCast cast) {
			cast.pay(HELD_COST);
		}
	}
}
