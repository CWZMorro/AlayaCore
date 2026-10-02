package io.github.cwzmorro.alayacore.shield;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.mixin.ProjectileInvoker;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankConfig;
import io.github.cwzmorro.alayacore.rank.RankLetter;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A shield (plan 9): an entity with HP from its rank that nothing gets through until it breaks. Content mods extend
 * it with their own shape, look and rules (Rho Aias, Avalon) and register it as their own entity type.
 *
 * <p>The ability that casts it calls {@link #deploy}, and {@link #hold} every tick it is held. It faces where it
 * looks ({@link #getLookAngle()}). Attacks that reach it from the side it stops damage it, aimed at it or not:
 * projectiles that would cross it hit it instead, a hit on anything it protects lands on it ({@link Shields}), and
 * explosions on its far side don't break blocks behind it.
 */
public abstract class ShieldEntity extends Entity {
	private static final EntityDataAccessor<Float> DATA_HP = SynchedEntityData.defineId(ShieldEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_MAX_HP = SynchedEntityData.defineId(ShieldEntity.class, EntityDataSerializers.FLOAT);
	/** Farthest a projectile moves in one tick that a shield still catches, in blocks. */
	private static final double PROJECTILE_REACH = 8.0;

	/** Set by {@link #deploy} (or loading); the lowest rank until then. */
	private Rank rank = new Rank(RankLetter.E, 0);
	private long expiresAt;

	protected ShieldEntity(EntityType<? extends ShieldEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/** Its outline: what an attack has to cross to reach what it protects. */
	public abstract ShieldShape shape();

	/** Blocks everything whatever the attack's rank (Avalon): never broken at once by a far higher rank. */
	protected boolean isAbsoluteDefence() {
		return false;
	}

	/** HP at full: 1.5 × the NP base damage of its rank by default (plan 13.7). */
	protected double maxHpFor(Rank rank) {
		return AlayaConfig.ranks().shieldHp(rank);
	}

	/** The HP a hit takes off before the rank rule; a shield's own rules change it here (Rho Aias: projectiles 50%). */
	protected double damageTaken(DamageSource source, float amount) {
		return amount;
	}

	/** Puts it up at full HP; it stays the tap time ({@code [shields] tap_seconds}). */
	public void deploy(Rank rank) {
		this.rank = rank;
		double max = this.maxHpFor(rank);
		this.entityData.set(DATA_MAX_HP, (float) max);
		this.setHp(max);
		this.expiresAt = this.level().getGameTime() + AlayaConfig.shields().tapTicks();
		this.fitBox();
	}

	public Rank rank() {
		return this.rank;
	}

	public double hp() {
		return this.entityData.get(DATA_HP);
	}

	public double maxHp() {
		return this.entityData.get(DATA_MAX_HP);
	}

	/** Ticks until it goes away on its own. */
	public long ticksLeft() {
		return Math.max(0L, this.expiresAt - this.level().getGameTime());
	}

	/** Where the path from {@code from} to {@code to} enters it, or null if it doesn't. */
	public @Nullable Vec3 entry(Vec3 from, Vec3 to) {
		return this.shape().entry(this.position(), this.getLookAngle(), from, to);
	}

	/**
	 * One tick of being held (plan 9), called by the ability while it is held: the holder pays the upkeep
	 * ({@code [shields] hold_upkeep_per_second}), then it gets back all the HP it is missing,
	 * or as much as the rest of the mana pays for at its rank's rate (13.7). It stays up while held, and the tap time
	 * once let go. A shield nobody holds never repairs.
	 */
	public void hold(LivingEntity holder) {
		this.expiresAt = this.level().getGameTime() + AlayaConfig.shields().tapTicks();
		ManaData mana = AlayaMana.get(holder);
		if (mana == null) {
			return;
		}
		RankConfig ranks = AlayaConfig.ranks();
		double upkeep = Math.min(mana.current(), AlayaConfig.shields().holdUpkeepPerTick());
		double repaired = ShieldRules.repairable(this.hp(), this.maxHp(), mana.current() - upkeep, ranks.manaRate(this.rank));
		AlayaMana.addCurrent(holder, -(upkeep + ranks.shieldRepairCost(this.rank, repaired)));
		this.setHp(this.hp() + repaired);
	}

	/** Takes a hit that reaches it from the side it stops; anything else (from behind, fire, falling) does nothing. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			this.discard();
			return true;
		}
		// An explosion's side was checked before it got here (ignoreExplosion).
		if (!source.is(DamageTypeTags.IS_EXPLOSION)) {
			Vec3 from = Shields.origin(source);
			if (from == null || this.entry(from, this.position()) == null) {
				return false;
			}
		}
		AttackData attack = Attacks.get(source.getDirectEntity());
		double hp = ShieldRules.hpAfterHit(attack == null ? null : attack.rank(), this.rank, this.isAbsoluteDefence(), this.hp(),
			this.damageTaken(source, amount));
		this.setHp(hp);
		if (hp <= 0) {
			this.discard();
		}
		return true;
	}

	/** An explosion on its far side doesn't touch it. */
	@Override
	public boolean ignoreExplosion(Explosion explosion) {
		return this.entry(explosion.center(), this.position()) == null;
	}

	@Override
	public void tick() {
		super.tick();
		this.fitBox();
		if (this.level().isClientSide()) {
			return;
		}
		if (this.ticksLeft() == 0) {
			this.discard();
			return;
		}
		this.catchProjectiles();
	}

	/** A projectile that crossed it last tick, or is about to this tick, hits it at the crossing instead. */
	private void catchProjectiles() {
		AABB reach = this.getBoundingBox().inflate(PROJECTILE_REACH);
		for (Projectile projectile : this.level().getEntitiesOfClass(Projectile.class, reach, Entity::isAlive)) {
			Vec3 now = projectile.position();
			Vec3 crossing = this.entry(Shields.lastPosition(projectile), now);
			if (crossing == null) {
				crossing = this.entry(now, now.add(projectile.getDeltaMovement()));
			}
			if (crossing != null) {
				projectile.setPos(crossing);
				((ProjectileInvoker) projectile).alayacore$hitTargetOrDeflectSelf(new EntityHitResult(this, crossing));
			}
		}
	}

	/** Its box follows its shape and facing, so it is hit where it is drawn. */
	private void fitBox() {
		this.setBoundingBox(this.shape().bounds(this.position(), this.getLookAngle()));
	}

	private void setHp(double hp) {
		this.entityData.set(DATA_HP, (float) hp);
	}

	/** It hangs where it was cast. */
	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	/** Projectiles are caught by its exact shape instead ({@link #tick}), never by its box. */
	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_HP, 0.0F);
		builder.define(DATA_MAX_HP, 0.0F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.store("rank", Rank.CODEC, this.rank);
		output.putFloat("hp", (float) this.hp());
		output.putFloat("max_hp", (float) this.maxHp());
		output.putLong("expires_at", this.expiresAt);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.rank = input.read("rank", Rank.CODEC).orElse(this.rank);
		this.entityData.set(DATA_MAX_HP, input.getFloatOr("max_hp", 0.0F));
		this.entityData.set(DATA_HP, input.getFloatOr("hp", 0.0F));
		this.expiresAt = input.getLongOr("expires_at", 0L);
	}
}
