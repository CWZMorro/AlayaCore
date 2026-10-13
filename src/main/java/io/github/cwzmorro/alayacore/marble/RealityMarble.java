package io.github.cwzmorro.alayacore.marble;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a content mod's reality marble exposed through a {@link RealityMarbleSource}.
 * Implementations manage their own terrain and effects, disabling abilities when broken.
 */
public interface RealityMarble {
	/** Where a marble is in its life; only an opening or standing marble is intact. */
	enum Phase {
		OPENING,
		STANDING,
		BROKEN,
		CLOSING
	}

	LivingEntity caster();

	ResourceKey<Level> dimension();

	Vec3 center();

	double radius();

	Phase phase();

	/**
	 * Returns the caster's servant at opening, or empty for none; changing servant closes the marble.
	 */
	Optional<Identifier> servant();

	/**
	 * Returns current mana for overlap comparisons; creative free casting does not increase strength.
	 */
	default double strength() {
		return RealityMarbles.strength(this.caster());
	}

	default boolean contains(Vec3 point) {
		return MarbleSphere.contains(this.center(), this.radius(), point);
	}

	/**
	 * Breaks an intact marble at an anti-world impact and disables its abilities; returns whether it broke.
	 */
	boolean shatter(Vec3 impact);

	/**
	 * Closes the marble and restores its terrain, returning false if closing must be retried.
	 */
	boolean close();
}
