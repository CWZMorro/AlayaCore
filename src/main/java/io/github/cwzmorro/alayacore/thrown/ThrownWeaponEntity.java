package io.github.cwzmorro.alayacore.thrown;

import com.mojang.serialization.Codec;
import io.github.cwzmorro.alayacore.combat.AttackTags;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.shield.Shields;
import java.util.UUID;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Carries a thrown item to its first impact, using weapon damage or a launcher-supplied override.
 * Launchers can customize flight and retention; {@link #INTERCEPT} listeners can take over movement.
 */
public class ThrownWeaponEntity extends Entity {
	/**
	 * Intercepts a flying weapon before impact. Returning true suppresses its ordinary hit and movement for this tick.
	 */
	@FunctionalInterface
	public interface Interceptor {
		boolean intercept(ServerLevel level, ThrownWeaponEntity weapon, Vec3 from, Vec3 to, BlockHitResult block, Vec3 firstImpact);
	}

	/** A launcher's own flight for one weapon, on the server, each tick it flies; not saved, so a reloaded one flies straight on. */
	@FunctionalInterface
	public interface Flight {
		boolean fly(ServerLevel level, ThrownWeaponEntity weapon);
	}

	public static final Event<Interceptor> INTERCEPT = EventFactory.createArrayBacked(Interceptor.class,
		listeners -> (level, weapon, from, to, block, firstImpact) -> {
			for (Interceptor listener : listeners) {
				if (listener.intercept(level, weapon, from, to, block, firstImpact)) {
					return true;
				}
			}
			return false;
		});
	private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_FORMATION = SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_FORMATION_DURATION = SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.INT);
	// A hand-thrown weapon's side (1 right, -1 left; 0 for everything else) and the game time it was thrown, for its spin.
	private static final EntityDataAccessor<Integer> DATA_THROW_SIDE = SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> DATA_THROW_START = SynchedEntityData.defineId(ThrownWeaponEntity.class, EntityDataSerializers.LONG);
	// Box in blocks; clients see it within this many chunks, and its place is sent every this many ticks.
	private static final float BOX = 0.5F;
	private static final int TRACKING_CHUNKS = 8;
	private static final int UPDATE_TICKS = 1;
	// Falling: the pull a tick and the air's drag, as for arrows and tridents.
	private static final double GRAVITY = 0.05;
	private static final double DRAG = 0.99;
	private static final Vec3 BOUNCE = new Vec3(-0.01, -0.1, -0.01);
	private static final float HIT_MARGIN = 0.3F;
	private static final double SINK = 0.05;
	private static final double SUPPORT = 0.06;

	enum State {
		FLYING, FALLING, STUCK
	}

	private @Nullable UUID owner;
	private long vanishAt;
	private double thrownFromY;
	private ResourceKey<DamageType> damageType = DamageTypes.GENERIC;
	private @Nullable Float fixedDamage;
	private float blastPower;
	private boolean held;
	private boolean saved = true;
	private @Nullable Flight flight;

	public ThrownWeaponEntity(EntityType<? extends ThrownWeaponEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/** Creates a type builder with shared dimensions, fire immunity and tracking settings. */
	public static <T extends ThrownWeaponEntity> EntityType.Builder<T> type(EntityType.EntityFactory<T> factory) {
		return EntityType.Builder.of(factory, MobCategory.MISC).sized(BOX, BOX).fireImmune().clientTrackingRange(TRACKING_CHUNKS)
			.updateInterval(UPDATE_TICKS);
	}

	/**
	 * Throws {@code item} as a {@code type} for {@code owner} from {@code from} at {@code velocity}, used up where it strikes or
	 * lands; {@code damageType} names it in death messages.
	 */
	public static ThrownWeaponEntity launch(ServerLevel level, EntityType<ThrownWeaponEntity> type, LivingEntity owner, ItemStack item,
		Vec3 from, Vec3 velocity, ResourceKey<DamageType> damageType) {
		ThrownWeaponEntity weapon = new ThrownWeaponEntity(type, level);
		weapon.damageType = damageType;
		weapon.throwFrom(owner, item, from, velocity);
		level.addFreshEntity(weapon);
		return weapon;
	}

	protected void throwFrom(LivingEntity owner, ItemStack item, Vec3 from, Vec3 velocity) {
		this.entityData.set(DATA_ITEM, item.copyWithCount(1));
		this.entityData.set(DATA_SCALE, owner.getScale());
		this.owner = owner.getUUID();
		this.vanishAt = this.level().getGameTime() + AlayaConfig.thrownWeapons().lifetimeTicks();
		this.thrownFromY = from.y;
		this.setPos(from);
		this.setDeltaMovement(velocity);
		face(this, velocity);
	}

	public static void face(Entity entity, Vec3 direction) {
		Vec3 unit = direction.normalize();
		entity.setYRot((float) (Mth.atan2(-unit.x, unit.z) * Mth.RAD_TO_DEG));
		entity.setXRot((float) (-Math.asin(Mth.clamp(unit.y, -1.0, 1.0)) * Mth.RAD_TO_DEG));
	}

	public void fixedDamage(float damage) {
		this.fixedDamage = damage;
	}

	public void blast(float power) {
		this.blastPower = power;
	}

	public void sized(float factor) {
		this.entityData.set(DATA_SCALE, this.scale() * factor);
	}

	/** Holds it in place for {@code ticks} as it forms; its aimed velocity and all later flight rules stay unchanged. */
	public void form(int ticks) {
		this.entityData.set(DATA_FORMATION_DURATION, ticks);
		this.entityData.set(DATA_FORMATION, ticks);
	}

	public void hold() {
		this.held = true;
	}

	public void unsaved() {
		this.saved = false;
	}

	public void spin(int side) {
		this.entityData.set(DATA_THROW_SIDE, side);
		this.entityData.set(DATA_THROW_START, this.level().getGameTime());
	}

	public @Nullable Flight flight() {
		return this.flight;
	}

	public void setFlight(@Nullable Flight flight) {
		this.flight = flight;
	}

	public boolean heldProjection() {
		return this.held;
	}

	/** Releases a held weapon at the given velocity and starts a disposable weapon's timeout. */
	public void releaseProjection(Vec3 velocity) {
		if (this.held && this.level() instanceof ServerLevel level) {
			this.vanishAt = level.getGameTime() + AlayaConfig.thrownWeapons().lifetimeTicks();
		}
		this.held = false;
		this.setDeltaMovement(velocity);
		face(this, velocity);
	}

	/** Stops flight, dropping a kept weapon with a fresh timeout or discarding a disposable one. */
	public void stop(ServerLevel level) {
		this.setDeltaMovement(Vec3.ZERO);
		this.setState(State.FALLING);
		if (this.kept()) {
			this.landed(level);
		} else {
			this.discard();
		}
	}

	public int throwSide() {
		return this.entityData.get(DATA_THROW_SIDE);
	}

	public float throwAge(float partialTick) {
		return this.level().getGameTime() - this.entityData.get(DATA_THROW_START) + partialTick;
	}

	public int formationRemaining() {
		return this.entityData.get(DATA_FORMATION);
	}

	public float formation(float partialTick) {
		int duration = this.entityData.get(DATA_FORMATION_DURATION);
		return duration == 0 ? 1 : Math.min(1, (duration - this.formationRemaining() + partialTick) / duration);
	}

	public ItemStack item() {
		return this.entityData.get(DATA_ITEM);
	}

	void setItem(ItemStack item) {
		this.entityData.set(DATA_ITEM, item);
		if (this.level() instanceof ServerLevel level) {
			this.itemChanged(level, item);
		}
	}

	public float scale() {
		return this.entityData.get(DATA_SCALE);
	}

	/** Returns whether the weapon is in flight rather than stopped or stuck. */
	public boolean flying() {
		return this.state() == State.FLYING;
	}

	protected boolean stuck() {
		return this.state() == State.STUCK;
	}

	private State state() {
		return State.values()[this.entityData.get(DATA_STATE)];
	}

	private void setState(State state) {
		this.entityData.set(DATA_STATE, (byte) state.ordinal());
	}

	/** Returns the owner's UUID on the server, or null on the client. */
	public @Nullable UUID owner() {
		return this.owner;
	}

	ResourceKey<DamageType> damageType() {
		return this.damageType;
	}

	/** Sets the damage type used in death messages. */
	protected void damageType(ResourceKey<DamageType> type) {
		this.damageType = type;
	}

	@Nullable Float fixedDamage() {
		return this.fixedDamage;
	}

	double thrownFromY() {
		return this.thrownFromY;
	}

	protected boolean timeUp() {
		return this.owner != null && this.level().getGameTime() >= this.vanishAt;
	}

	/** Handles timeout or falling out of the world; discards the weapon by default. */
	protected void expire(ServerLevel level) {
		this.discard();
	}

	/** Returns whether the weapon stays at its landing position; false by default. */
	protected boolean kept() {
		return false;
	}

	/** Handles a kept weapon landing or stopping in flight. */
	protected void landed(ServerLevel level) {
	}

	/** Handles permanent loss of the item through breakage or consumption. */
	protected void forget(ServerLevel level) {
	}

	/** Handles an item change after a strike wears the weapon. */
	protected void itemChanged(ServerLevel level, ItemStack item) {
	}

	@Override
	public void tick() {
		super.tick();
		State state = this.state();
		ServerLevel server = this.level() instanceof ServerLevel level ? level : null;
		if (server != null && !this.held && this.timeUp()) {
			this.expire(server);
			return;
		}
		if (state == State.STUCK) {
			if (server != null) {
				this.lie(server);
			}
			return;
		}
		if (this.formationRemaining() > 0) {
			if (server != null) {
				this.entityData.set(DATA_FORMATION, this.formationRemaining() - 1);
			}
			return;
		}
		if (this.held) {
			return;
		}
		if (server != null && this.flight != null && state == State.FLYING && this.flight.fly(server, this)) {
			return;
		}
		Vec3 velocity = this.getDeltaMovement();
		if (state == State.FALLING) {
			velocity = velocity.scale(DRAG).add(0, -GRAVITY, 0);
		}
		Vec3 from = this.position();
		Vec3 to = from.add(velocity);
		BlockHitResult block = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if (block.getType() != HitResult.Type.MISS) {
			to = block.getLocation();
		}
		if (server != null && this.owner() != null) {
			if (state == State.FLYING) {
				Shields.Hit shield = Shields.firstHit(server, from, to);
				EntityHitResult hit = ProjectileUtil.getEntityHitResult(server, this, from, shield == null ? to : shield.entry(),
					this.getBoundingBox().expandTowards(velocity).inflate(1.0), this::canStrike, HIT_MARGIN);
				Vec3 firstImpact = hit != null ? hit.getLocation() : shield != null ? shield.entry() : to;
				if (INTERCEPT.invoker().intercept(server, this, from, from.add(velocity), block, firstImpact)) {
					return;
				}
				if (hit != null) {
					this.setPos(hit.getLocation());
					this.struck(server, ThrownWeaponDamage.strike(server, this, hit.getEntity(), velocity), velocity);
					return;
				}
				if (shield != null) {
					this.setPos(shield.entry());
					this.struck(server, ThrownWeaponDamage.strike(server, this, shield, velocity), velocity);
					return;
				}
			}
			if (block.getType() != HitResult.Type.MISS) {
				this.stick(server, block, velocity);
				return;
			}
		}
		this.setDeltaMovement(block.getType() == HitResult.Type.MISS ? velocity : Vec3.ZERO);
		this.setPos(to);
		if (block.getType() == HitResult.Type.MISS) {
			face(this, velocity);
		}
	}

	private boolean canStrike(Entity entity) {
		return entity.isAlive() && entity.isPickable() && !entity.isSpectator() && !AttackTags.thrownWeaponsPassThrough(entity)
			&& (this.owner() == null || !entity.getUUID().equals(this.owner()));
	}

	private void struck(ServerLevel level, boolean broken, Vec3 velocity) {
		if (broken || !this.kept()) {
			this.forget(level);
			this.discard();
			return;
		}
		this.setDeltaMovement(velocity.multiply(BOUNCE));
		this.setState(State.FALLING);
	}

	private void stick(ServerLevel level, BlockHitResult block, Vec3 velocity) {
		if (!this.kept()) {
			if (this.blastPower > 0) {
				Vec3 point = block.getLocation();
				level.explode(this, point.x, point.y, point.z, this.blastPower,
					level.getGameRules().get(AlayaGameRules.WEAPON_BLOCK_DAMAGE) ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
			}
			this.forget(level);
			this.discard();
			return;
		}
		this.setPos(block.getLocation().add(velocity.normalize().scale(SINK)));
		this.setDeltaMovement(Vec3.ZERO);
		this.landed(level);
		this.setState(State.STUCK);
	}

	private void lie(ServerLevel level) {
		if (level.noCollision(new AABB(this.position(), this.position()).inflate(SUPPORT))) {
			this.setState(State.FALLING);
		}
	}

	@Override
	public void onBelowWorld() {
		if (this.level() instanceof ServerLevel level) {
			this.expire(level);
		}
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return super.getDimensions(pose).scale(this.scale());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_SCALE.equals(accessor)) {
			this.refreshDimensions();
		}
	}

	@Override
	protected Component getTypeName() {
		return this.item().getHoverName();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isNoGravity() {
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ITEM, ItemStack.EMPTY);
		builder.define(DATA_STATE, (byte) State.FLYING.ordinal());
		builder.define(DATA_SCALE, 1.0F);
		builder.define(DATA_FORMATION, 0);
		builder.define(DATA_FORMATION_DURATION, 0);
		builder.define(DATA_THROW_SIDE, 0);
		builder.define(DATA_THROW_START, 0L);
	}

	@Override
	public boolean shouldBeSaved() {
		return this.saved && super.shouldBeSaved();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.store("item", ItemStack.OPTIONAL_CODEC, this.item());
		output.putFloat("scale", this.scale());
		output.putInt("formation", this.formationRemaining());
		output.putInt("formation_duration", this.entityData.get(DATA_FORMATION_DURATION));
		output.putInt("throw_side", this.throwSide());
		output.putLong("throw_start", this.entityData.get(DATA_THROW_START));
		output.putDouble("thrown_from_y", this.thrownFromY);
		if (this.owner != null) {
			output.store("owner", UUIDUtil.CODEC, this.owner);
		}
		output.putLong("vanish_at", this.vanishAt);
		output.store("damage_type", ResourceKey.codec(Registries.DAMAGE_TYPE), this.damageType);
		if (this.fixedDamage != null) {
			output.putFloat("damage", this.fixedDamage);
		}
		output.putFloat("blast_power", this.blastPower);
		output.putByte("state", this.entityData.get(DATA_STATE));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.entityData.set(DATA_ITEM, input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
		this.entityData.set(DATA_SCALE, input.getFloatOr("scale", 1.0F));
		this.entityData.set(DATA_FORMATION, Math.max(0, input.getIntOr("formation", 0)));
		this.entityData.set(DATA_FORMATION_DURATION, Math.max(0, input.getIntOr("formation_duration", 0)));
		this.entityData.set(DATA_THROW_SIDE, input.getIntOr("throw_side", 0));
		this.entityData.set(DATA_THROW_START, input.getLongOr("throw_start", 0L));
		this.thrownFromY = input.getDoubleOr("thrown_from_y", this.getY());
		this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		this.vanishAt = input.getLongOr("vanish_at", 0L);
		this.damageType = input.read("damage_type", ResourceKey.codec(Registries.DAMAGE_TYPE)).orElse(this.damageType);
		this.fixedDamage = input.read("damage", Codec.FLOAT).orElse(null);
		this.blastPower = input.getFloatOr("blast_power", 0);
		this.entityData.set(DATA_STATE, (byte) Math.floorMod(input.getByteOr("state", (byte) 0), State.values().length));
	}
}
