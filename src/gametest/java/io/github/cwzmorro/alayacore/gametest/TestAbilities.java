package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityCast;
import io.github.cwzmorro.alayacore.ability.AbilitySetting;
import io.github.cwzmorro.alayacore.ability.PassiveAbility;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.rank.Rank;
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

final class TestAbilities {
	static final ResourceKey<Ability> RECORDER = key("recorder");
	static final ResourceKey<Ability> EXCLUSIVE = key("exclusive");
	static final ResourceKey<Ability> NOBLE_PHANTASM = key("noble_phantasm");
	static final ResourceKey<Ability> COOLDOWN = key("cooldown");
	static final ResourceKey<Ability> COSTLY = key("costly");
	static final ResourceKey<Ability> PICKY = key("picky");

	static final ResourceKey<Ability> DEFLECTING = key("deflecting");

	static final ResourceKey<Ability> CONFIGURABLE = key("configurable");
	static final AbilitySetting REACH = new AbilitySetting("reach", 1.0, 5.0, 0.5, 2.0);

	static final ResourceKey<Ability> RANGED = key("ranged");
	static final ResourceKey<Ability> UNKNOWN = key("unknown");
	static final ResourceKey<Ability> PASSIVE = key("passive");

	static final double COOLDOWN_SECONDS = 1.0;
	static final double START_COST = 500.0;
	static final double HELD_COST = 100.0;
	static final int RECORDER_MODES = 2;

	private static final Map<UUID, List<String>> EVENTS = new ConcurrentHashMap<>();

	private TestAbilities() {
	}

	private static ResourceKey<Ability> key(String path) {
		return ResourceKey.create(AlayaRegistries.ABILITY_KEY, TestServants.id(path));
	}

	static void register() {
		Registry.register(AlayaRegistries.ABILITY, RECORDER, new Recorder(new Ability.Properties().modes(RECORDER_MODES)));
		Registry.register(AlayaRegistries.ABILITY, EXCLUSIVE, new Recorder(new Ability.Properties().exclusive()));
		Registry.register(AlayaRegistries.ABILITY, NOBLE_PHANTASM, new Recorder(new Ability.Properties().noblePhantasm()));
		Registry.register(AlayaRegistries.ABILITY, COOLDOWN, new Recorder(new Ability.Properties().cooldownSeconds(COOLDOWN_SECONDS)));
		Registry.register(AlayaRegistries.ABILITY, COSTLY, new Costly());
		Registry.register(AlayaRegistries.ABILITY, PICKY, new Picky());
		Registry.register(AlayaRegistries.ABILITY, DEFLECTING, new Deflecting());
		Registry.register(AlayaRegistries.ABILITY, CONFIGURABLE, new Configurable());
		Registry.register(AlayaRegistries.ABILITY, RANGED, new Ranked(Rank.range(Rank.parse("E"), Rank.parse("A++"))));
		Registry.register(AlayaRegistries.ABILITY, UNKNOWN, new Ranked(Ability.UNKNOWN_RANK));
		Registry.register(AlayaRegistries.ABILITY, PASSIVE, new PassiveAbility());
	}

	static List<String> events(LivingEntity entity) {
		return List.copyOf(EVENTS.getOrDefault(entity.getUUID(), List.of()));
	}

	private static final class Configurable extends Recorder {
		private Configurable() {
			super(new Ability.Properties());
		}

		@Override
		public List<AbilitySetting> settings() {
			return List.of(REACH);
		}
	}

	private static final class Deflecting extends Recorder {
		private Deflecting() {
			super(new Ability.Properties());
		}

		@Override
		public boolean deflects() {
			return true;
		}
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
		return event(ability.identifier(), hook);
	}

	private static String event(Identifier ability, String hook) {
		return ability.getPath() + ":" + hook;
	}

	private static class Recorder extends Ability {
		Recorder(Properties properties) {
			super(properties);
		}

		private static void record(AbilityCast cast, String hook) {
			EVENTS.computeIfAbsent(cast.entity().getUUID(), uuid -> Collections.synchronizedList(new ArrayList<>()))
				.add(event(cast.abilityId(), hook));
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

	private static final class Ranked extends Ability {
		private final String rankLabel;

		private Ranked(String rankLabel) {
			super(new Properties());
			this.rankLabel = rankLabel;
		}

		@Override
		public String rankLabel() {
			return this.rankLabel;
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
