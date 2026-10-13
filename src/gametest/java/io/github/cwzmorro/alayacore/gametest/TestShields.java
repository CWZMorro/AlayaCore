package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.AttackOrigin;
import io.github.cwzmorro.alayacore.shield.ClientShieldEvents;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import io.github.cwzmorro.alayacore.shield.ShieldShape;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class TestShields {
	private static final double DISC_RADIUS = 2.5;
	private static final double DOME_RADIUS = 3.0;
	private static final float SIZE = 0.5F;
	static final EntityType<TestShield> DISC = register("disc", new ShieldShape.Disc(DISC_RADIUS), false);
	static final EntityType<TestShield> SOLID_DISC = register("solid_disc", new ShieldShape.Disc(DISC_RADIUS), true);
	static final EntityType<TestShield> DOME = register("dome", new ShieldShape.Dome(DOME_RADIUS), false);
	static final EntityType<RemoteAttack> REMOTE_ATTACK = register("remote_attack", EntityType.Builder.of(RemoteAttack::new, MobCategory.MISC));

	private TestShields() {
	}

	static final class RemoteAttack extends Entity implements AttackOrigin {
		Vec3 from = Vec3.ZERO;

		private RemoteAttack(EntityType<RemoteAttack> type, Level level) {
			super(type, level);
		}

		@Override
		public Vec3 attackOrigin(Vec3 target) {
			return this.from;
		}

		@Override
		public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
			return false;
		}

		@Override
		protected void defineSynchedData(SynchedEntityData.Builder builder) {
		}

		@Override
		protected void addAdditionalSaveData(ValueOutput output) {
		}

		@Override
		protected void readAdditionalSaveData(ValueInput input) {
		}
	}

	static final class TestShield extends ShieldEntity {
		private final ShieldShape shape;
		private final boolean solid;
		@Nullable Vec3 hitPoint;
		double hpTaken;
		int hits;
		int looks;
		@Nullable UUID caster;

		private TestShield(EntityType<TestShield> type, Level level, ShieldShape shape, boolean solid) {
			super(type, level);
			this.shape = shape;
			this.solid = solid;
		}

		@Override
		public ShieldShape shape() {
			return this.shape;
		}

		@Override
		protected boolean solid() {
			return this.solid;
		}

		@Override
		protected @Nullable UUID casterUuid() {
			return this.caster;
		}

		@Override
		protected void onHit(DamageSource source, @Nullable AttackData attack, Vec3 point, double hpTaken) {
			this.hitPoint = point;
			this.hpTaken = hpTaken;
			this.hits++;
		}
	}

	private static EntityType<TestShield> register(String name, ShieldShape shape, boolean solid) {
		return register(name + "_shield", EntityType.Builder.<TestShield>of((type, level) -> new TestShield(type, level, shape, solid), MobCategory.MISC));
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alayacore-gametest", name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.sized(SIZE, SIZE).build(key));
	}

	static void init() {
		ClientShieldEvents.register(DISC, (shield, point, hpTaken) -> shield.looks++);
	}
}
