package io.github.cwzmorro.alayacore.clash;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server world clash resolution, before any participating beam deals its tick's hit. */
public final class BeamClashes {
	private static final double EPSILON = 1.0E-10;
	private static final double UNCUT = Double.POSITIVE_INFINITY;
	private static final double ORDINARY_DAMAGE = 1.0;
	private static final AttachmentType<BeamClashes> TRACKER = AttachmentRegistry.create(AlayaCore.id("beam_clashes"),
		builder -> builder.initializer(BeamClashes::new));
	private static final AttachmentType<State> STATE = AttachmentRegistry.create(AlayaCore.id("beam_clash_state"),
		builder -> builder.initializer(() -> new State(UNCUT, ORDINARY_DAMAGE)));

	private final List<Pair> pairs = new ArrayList<>();

	public BeamClashes() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				List<ClashBeam> beams = new ArrayList<>();
				for (Entity entity : level.getAllEntities()) {
					if (entity instanceof ClashBeam beam) {
						beams.add(beam);
					}
				}
				level.getAttachedOrCreate(TRACKER).resolve(level, beams);
			}
		});
	}

	public static double reach(Entity beam, double fullLength) {
		return Math.min(fullLength, beam.getAttachedOrCreate(STATE).cut());
	}

	public static float damage(Entity beam, float ordinaryDamage) {
		return (float) (ordinaryDamage * beam.getAttachedOrCreate(STATE).damageScale());
	}

	/** Resolves live pairs and then lets each survivor apply effects, independent of entity tick order. */
	public void resolve(ServerLevel level, List<ClashBeam> beams) {
		for (ClashBeam beam : beams) {
			State state = beam.beamEntity().getAttachedOrCreate(STATE);
			beam.beamEntity().setAttached(STATE, new State(UNCUT, state.damageScale()));
		}
		// Keep the previous pair long enough to recognize a release, death or exhausted cast.
		this.pairs.removeIf(pair -> {
			boolean a = live(pair.a(), level);
			boolean b = live(pair.b(), level);
			if (!a || !b) {
				if (a != b) {
					win(a ? pair.a() : pair.b(), a ? pair.b() : pair.a());
				}
				return true;
			}
			return !meet(pair.a(), pair.b());
		});
		for (int i = 0; i < beams.size(); i++) {
			ClashBeam a = beams.get(i);
			for (int j = i + 1; j < beams.size(); j++) {
				ClashBeam b = beams.get(j);
				AttackData attackA = Attacks.get(a.beamEntity());
				AttackData attackB = Attacks.get(b.beamEntity());
				if (!live(a, level) || !live(b, level) || attackA == null || attackB == null
					|| !ClashRules.clash(attackA, attackB) || !meet(a, b)) {
					continue;
				}
				ClashRules.Side outright = ClashRules.outrightWinner(attackA.rank(), attackB.rank());
				if (outright != null) {
					win(outright == ClashRules.Side.A ? a : b, outright == ClashRules.Side.A ? b : a);
					continue;
				}
				Pair pair = new Pair(a, b);
				if (!this.pairs.contains(pair) && !this.pairs.contains(new Pair(b, a))) {
					this.pairs.add(pair);
				}
				double drainA = AlayaConfig.clash().drainPerSecond(a.beamDrainPerSecond(), attackA.rank(), attackB.rank());
				double drainB = AlayaConfig.clash().drainPerSecond(b.beamDrainPerSecond(), attackB.rank(), attackA.rank());
				payExtra(a, drainA);
				payExtra(b, drainB);
				double manaA = available(a);
				double manaB = available(b);
				if (manaA <= 0 || manaB <= 0) {
					if (manaA <= 0 && manaB <= 0) {
						a.loseClash();
						b.loseClash();
					} else {
						win(manaA > 0 ? a : b, manaA > 0 ? b : a);
					}
					continue;
				}
				double fraction = ClashRules.meetingPoint(ClashRules.secondsLeft(manaA, drainA), ClashRules.secondsLeft(manaB, drainB));
				Vec3 point = a.beamOrigin().lerp(b.beamOrigin(), fraction);
				cut(a, point);
				cut(b, point);
			}
		}
		for (ClashBeam beam : beams) {
			if (live(beam, level)) {
				beam.beamEffects(level);
			}
		}
	}

	private static boolean live(ClashBeam beam, ServerLevel level) {
		return !beam.beamEntity().isRemoved() && beam.beamEntity().level() == level && beam.beamLive();
	}

	private static void win(ClashBeam winner, ClashBeam loser) {
		loser.loseClash();
		State state = winner.beamEntity().getAttachedOrCreate(STATE);
		winner.beamEntity().setAttached(STATE, new State(state.cut(), AlayaConfig.clash().winnerContinuationDamage(ORDINARY_DAMAGE)));
	}

	private static double available(ClashBeam beam) {
		LivingEntity owner = beam.beamOwner();
		return owner == null ? 0.0 : AlayaMana.available(owner);
	}

	private static void payExtra(ClashBeam beam, double drain) {
		LivingEntity owner = beam.beamOwner();
		if (owner != null) {
			AlayaMana.spend(owner, Math.max(0.0, drain - beam.beamDrainPerSecond()) / SharedConstants.TICKS_PER_SECOND);
		}
	}

	private static void cut(ClashBeam beam, Vec3 point) {
		Entity entity = beam.beamEntity();
		State state = entity.getAttachedOrCreate(STATE);
		double length = Mth.clamp(point.subtract(beam.beamOrigin()).dot(beam.beamDirection()), 0.0, beam.beamLength());
		entity.setAttached(STATE, new State(Math.min(state.cut(), length), state.damageScale()));
	}

	private static boolean meet(ClashBeam a, ClashBeam b) {
		Vec3 u = a.beamDirection().scale(a.beamLength());
		Vec3 v = b.beamDirection().scale(b.beamLength());
		Vec3 w = a.beamOrigin().subtract(b.beamOrigin());
		double uu = u.dot(u);
		double vv = v.dot(v);
		double uv = u.dot(v);
		double uw = u.dot(w);
		double vw = v.dot(w);
		double denominator = uu * vv - uv * uv;
		double s = denominator > EPSILON ? Mth.clamp((uv * vw - vv * uw) / denominator, 0.0, 1.0) : 0.0;
		double t = vv > EPSILON ? (uv * s + vw) / vv : 0.0;
		if (t < 0 || t > 1 || vv <= EPSILON) {
			t = Mth.clamp(t, 0.0, 1.0);
			s = uu > EPSILON ? Mth.clamp((uv * t - uw) / uu, 0.0, 1.0) : 0.0;
		}
		double radius = a.beamRadius() + b.beamRadius();
		return w.add(u.scale(s)).subtract(v.scale(t)).lengthSqr() <= radius * radius;
	}

	private record Pair(ClashBeam a, ClashBeam b) {
	}

	private record State(double cut, double damageScale) {
	}
}
