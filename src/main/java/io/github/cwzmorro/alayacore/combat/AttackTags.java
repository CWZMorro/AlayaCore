package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.shield.ShieldEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;

/** Entity type tags the attacks of every content mod share; content mods add their own entities to them. */
public final class AttackTags {
	/** Flying things that are not {@link Projectile}s but a deflecting attack pushes away too, such as content mods' thrown weapons. */
	public static final TagKey<EntityType<?>> DEFLECTABLE = TagKey.create(Registries.ENTITY_TYPE, AlayaCore.id("deflectable"));
	/** What a thrown weapon flies through without striking it, such as other thrown weapons; {@link #thrownWeaponsPassThrough} adds shields. */
	public static final TagKey<EntityType<?>> THROWN_WEAPONS_PASS_THROUGH = TagKey.create(Registries.ENTITY_TYPE,
		AlayaCore.id("thrown_weapons_pass_through"));

	private AttackTags() {
	}

	/** Returns whether the entity is a projectile or belongs to {@link #DEFLECTABLE}. */
	public static boolean deflectable(Entity entity) {
		return entity instanceof Projectile || entity.is(DEFLECTABLE);
	}

	/**
	 * Returns whether ordinary thrown-weapon collision skips this entity; shields use shape interception separately.
	 */
	public static boolean thrownWeaponsPassThrough(Entity entity) {
		return entity instanceof ShieldEntity || entity.is(THROWN_WEAPONS_PASS_THROUGH);
	}
}
