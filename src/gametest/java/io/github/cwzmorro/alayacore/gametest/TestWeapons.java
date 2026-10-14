package io.github.cwzmorro.alayacore.gametest;

import io.github.cwzmorro.alayacore.thrown.ThrownWeaponEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

final class TestWeapons {
	static final EntityType<ThrownWeaponEntity> THROWN = register();

	private TestWeapons() {
	}

	private static EntityType<ThrownWeaponEntity> register() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, TestServants.id("thrown_weapon"));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, ThrownWeaponEntity.<ThrownWeaponEntity>type(ThrownWeaponEntity::new).build(key));
	}

	static void init() {
	}
}
