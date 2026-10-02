package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import io.github.cwzmorro.alayacore.shield.ShieldShape;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

/** Shields used only by the game tests: a disc like Rho Aias and a dome like Lord Camelot. */
final class TestShields {
	static final double DISC_RADIUS = 2.5;
	static final double DOME_RADIUS = 3.0;
	/** The entity's own box: a point the shapes are measured from. */
	private static final float SIZE = 0.5F;
	static final EntityType<TestShield> DISC = register("disc", new ShieldShape.Disc(DISC_RADIUS));
	static final EntityType<TestShield> DOME = register("dome", new ShieldShape.Dome(DOME_RADIUS));

	private TestShields() {
	}

	static final class TestShield extends ShieldEntity {
		private final ShieldShape shape;

		private TestShield(EntityType<TestShield> type, Level level, ShieldShape shape) {
			super(type, level);
			this.shape = shape;
		}

		@Override
		public ShieldShape shape() {
			return this.shape;
		}
	}

	private static EntityType<TestShield> register(String name, ShieldShape shape) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alayacore-gametest", name + "_shield"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<TestShield>of((type, level) -> new TestShield(type, level, shape),
			MobCategory.MISC).sized(SIZE, SIZE).build(key));
	}

	/** Registers the shield types (loading this class does it). */
	static void init() {
	}
}
