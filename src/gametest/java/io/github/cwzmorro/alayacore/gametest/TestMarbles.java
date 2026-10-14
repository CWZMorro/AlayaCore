package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.marble.RealityMarble;
import io.github.cwzmorro.alayacore.marble.RealityMarbles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class TestMarbles {
	private static final List<TestMarble> OPEN = new ArrayList<>();

	private TestMarbles() {
	}

	static final class TestMarble implements RealityMarble {
		private final LivingEntity caster;
		private final ResourceKey<Level> dimension;
		private final Vec3 center;
		private final double radius;
		private final Optional<Identifier> servant;
		private Phase phase = Phase.STANDING;
		@Nullable Vec3 impact;
		boolean closed;

		private TestMarble(LivingEntity caster, Vec3 center, double radius) {
			this.caster = caster;
			this.dimension = caster.level().dimension();
			this.center = center;
			this.radius = radius;
			this.servant = AlayaServants.servantId(caster);
		}

		@Override
		public LivingEntity caster() {
			return this.caster;
		}

		@Override
		public ResourceKey<Level> dimension() {
			return this.dimension;
		}

		@Override
		public Vec3 center() {
			return this.center;
		}

		@Override
		public double radius() {
			return this.radius;
		}

		@Override
		public Phase phase() {
			return this.phase;
		}

		@Override
		public Optional<Identifier> servant() {
			return this.servant;
		}

		@Override
		public boolean shatter(Vec3 impact) {
			this.phase = Phase.BROKEN;
			this.impact = impact;
			return true;
		}

		@Override
		public boolean close() {
			OPEN.remove(this);
			this.phase = Phase.CLOSING;
			this.closed = true;
			return true;
		}
	}

	record Opening(RealityMarbles.Verdict verdict, @Nullable TestMarble marble) {
	}

	static void register() {
		RealityMarbles.register(server -> List.copyOf(OPEN));
	}

	static Opening open(ServerLevel level, LivingEntity caster, Vec3 center, double radius) {
		RealityMarbles.Overlap overlap = RealityMarbles.overlap(level, caster, center, radius);
		if (overlap.verdict() != RealityMarbles.Verdict.CLEAR) {
			return new Opening(overlap.verdict(), null);
		}
		overlap.losers().forEach(RealityMarble::close);
		TestMarble marble = new TestMarble(caster, center, radius);
		OPEN.add(marble);
		return new Opening(overlap.verdict(), marble);
	}
}
