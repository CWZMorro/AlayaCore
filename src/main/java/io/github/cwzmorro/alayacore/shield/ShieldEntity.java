package io.github.cwzmorro.alayacore.shield;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mixin.ProjectileInvoker;
import io.github.cwzmorro.alayacore.network.ShieldHitPayload;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.rank.RankConfig;
import io.github.cwzmorro.alayacore.rank.RankLetter;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Provides a ranked shield for content mods to extend. Abilities call {@link #deploy}, then {@link #hold} each held tick.
 * Intercepts incoming attacks through {@link Shields} and reports hits to tracking clients.
 */
public abstract class ShieldEntity extends Entity {
	private static final EntityDataAccessor<Float> DATA_HP = SynchedEntityData.defineId(ShieldEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_MAX_HP = SynchedEntityData.defineId(ShieldEntity.class, EntityDataSerializers.FLOAT);
	// Farthest a projectile moves in one tick that a shield still catches, in blocks.
	private static final double PROJECTILE_REACH = 8.0;

	private Rank rank = new Rank(RankLetter.E, 0);
	private long expiresAt;
	// Exact crossing while vanilla's projectile hit handling calls back into hurtServer.
	private @Nullable Vec3 projectileImpact;

	protected ShieldEntity(EntityType<? extends ShieldEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	public abstract ShieldShape shape();

	public void deploy(Rank rank) {
		this.rank = rank;
		double max = this.maxHpFor(rank);
		this.entityData.set(DATA_MAX_HP, (float) max);
		this.setHp(max);
		this.expiresAt = this.level().getGameTime() + AlayaConfig.shields().tapTicks();
		this.fitBox();
	}

	/**
	 * Pays upkeep, repairs as much HP as the holder's remaining mana permits, and refreshes the tap timeout.
	 *
	 * @return false once the holder has no mana left to hold it with: the ability should let go
	 */
	public boolean hold(LivingEntity holder) {
		this.expiresAt = this.level().getGameTime() + AlayaConfig.shields().tapTicks();
		double available = AlayaMana.available(holder);
		RankConfig ranks = AlayaConfig.ranks();
		double upkeep = Math.min(available, AlayaConfig.shields().holdUpkeepPerTick());
		double repaired = ShieldRules.repairable(this.hp(), this.maxHp(), available - upkeep, ranks.manaRate(this.rank));
		AlayaMana.spend(holder, upkeep + ranks.shieldRepairCost(this.rank, repaired));
		this.setHp(this.hp() + repaired);
		return AlayaMana.available(holder) > 0;
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

	public long ticksLeft() {
		return Math.max(0L, this.expiresAt - this.level().getGameTime());
	}

	public @Nullable Vec3 entry(Vec3 from, Vec3 to) {
		return this.shape().entry(this.position(), this.getLookAngle(), from, to);
	}

	public Vec3 nearestPoint(Vec3 point) {
		return this.shape().nearestPoint(this.position(), this.getLookAngle(), point);
	}

	/**
	 * Applies an intercepted hit at its crossing point; callers must first check the blocked side with {@link #entry}.
	 */
	public boolean hurtAt(ServerLevel level, DamageSource source, float amount, Vec3 point) {
		if (this.isRemoved()) {
			return false;
		}
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			this.discard();
			return true;
		}
		AttackData attack = Attacks.get(source.getDirectEntity());
		double before = this.hp();
		double hp = ShieldRules.hpAfterHit(attack == null ? null : attack.rank(), this.rank, before, this.damageTaken(source, attack, amount));
		this.setHp(hp);
		double taken = before - this.hp();
		if (taken > 0) {
			ShieldHitPayload payload = new ShieldHitPayload(this.getId(), point, (float) taken);
			for (ServerPlayer player : PlayerLookup.tracking(this)) {
				ServerPlayNetworking.send(player, payload);
			}
			this.onHit(source, attack, point, taken);
		}
		if (hp <= 0) {
			this.discard();
		}
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			this.discard();
			return true;
		}
		if (this.projectileImpact != null) {
			return this.hurtAt(level, source, amount, this.projectileImpact);
		}
		Vec3 from = Shields.origin(source);
		// Vanilla explosions don't come here (ServerExplosionMixin calls hurtAt); other blasts land nearest where they came from.
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			return this.hurtAt(level, source, amount, this.nearestPoint(from == null ? this.position() : from));
		}
		Vec3 fallback = from == null ? null : this.entry(from, this.position());
		if (fallback == null) {
			return false;
		}
		Vec3 point = null;
		if (source.getDirectEntity() instanceof LivingEntity attacker) {
			Vec3 eye = attacker.getEyePosition();
			double reach = eye.distanceTo(this.position()) + this.shape().radius();
			point = this.entry(eye, eye.add(attacker.getLookAngle().scale(reach)));
		}
		return this.hurtAt(level, source, amount, point == null ? fallback : point);
	}

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

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	protected double maxHpFor(Rank rank) {
		return AlayaConfig.ranks().shieldHp(rank);
	}

	/**
	 * Returns damage before the rank rule, allowing subclasses to adjust incoming damage.
	 *
	 * @param attack the attack's data, or null for an attack without any
	 */
	protected double damageTaken(DamageSource source, @Nullable AttackData attack, float amount) {
		return amount;
	}

	/**
	 * Handles a damaging hit after client notification and before breakage, including HP lost to a rank break.
	 */
	protected void onHit(DamageSource source, @Nullable AttackData attack, Vec3 point, double hpTaken) {
	}

	/** Fits the box to the current shape and facing; moving shields call this after following their holder. */
	protected final void fitBox() {
		this.setBoundingBox(this.shape().bounds(this.position(), this.getLookAngle()));
	}

	private void setHp(double hp) {
		this.entityData.set(DATA_HP, (float) hp);
	}

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
				this.projectileImpact = crossing;
				try {
					((ProjectileInvoker) projectile).alayacore$hitTargetOrDeflectSelf(new EntityHitResult(this, crossing));
				} finally {
					this.projectileImpact = null;
				}
			}
		}
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
