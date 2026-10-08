package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import io.github.cwzmorro.alayacore.shield.ShieldShape;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class TestShields {
	private static final double DISC_RADIUS = 2.5;
	private static final double DOME_RADIUS = 3.0;
	private static final float SIZE = 0.5F;
	static final EntityType<TestShield> DISC = register("disc", new ShieldShape.Disc(DISC_RADIUS));
	static final EntityType<TestShield> DOME = register("dome", new ShieldShape.Dome(DOME_RADIUS));

	private TestShields() {
	}

	static final class TestShield extends ShieldEntity {
		private final ShieldShape shape;
		@Nullable Vec3 hitPoint;
		double hpTaken;
		int hits;

		private TestShield(EntityType<TestShield> type, Level level, ShieldShape shape) {
			super(type, level);
			this.shape = shape;
		}

		@Override
		public ShieldShape shape() {
			return this.shape;
		}

		@Override
		protected void onHit(DamageSource source, @Nullable AttackData attack, Vec3 point, double hpTaken) {
			this.hitPoint = point;
			this.hpTaken = hpTaken;
			this.hits++;
		}
	}

	private static EntityType<TestShield> register(String name, ShieldShape shape) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alayacore-gametest", name + "_shield"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<TestShield>of((type, level) -> new TestShield(type, level, shape),
			MobCategory.MISC).sized(SIZE, SIZE).build(key));
	}

	static void init() {
	}
}
