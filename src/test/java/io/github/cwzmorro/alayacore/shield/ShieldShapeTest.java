package io.github.cwzmorro.alayacore.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ShieldShapeTest {
	private static final double EPS = 1e-9;
	private static final Vec3 CENTRE = Vec3.ZERO;
	/** Faces +Z: attacks come from z > 0. */
	private static final Vec3 SOUTH = new Vec3(0, 0, 1);
	private static final ShieldShape DISC = new ShieldShape.Disc(2.5);
	private static final ShieldShape DOME = new ShieldShape.Dome(3);

	@Test
	void discStopsPathsFromTheFrontThroughIt() {
		Vec3 entry = DISC.entry(CENTRE, SOUTH, new Vec3(1, 0, 4), new Vec3(1, 0, -4));
		assertNotNull(entry);
		assertEquals(0.0, entry.z, EPS);
		assertEquals(1.0, entry.x, EPS);
	}

	@Test
	void discLetsPathsFromBehindOrAroundItThrough() {
		assertNull(DISC.entry(CENTRE, SOUTH, new Vec3(0, 0, -4), new Vec3(0, 0, 4)), "from behind");
		assertNull(DISC.entry(CENTRE, SOUTH, new Vec3(3, 0, 4), new Vec3(3, 0, -4)), "past its edge");
		assertNull(DISC.entry(CENTRE, SOUTH, new Vec3(0, 0, 4), new Vec3(0, 0, 1)), "stops in front");
	}

	@Test
	void domeStopsPathsComingIn() {
		Vec3 entry = DOME.entry(CENTRE, SOUTH, new Vec3(0, 0, 5), Vec3.ZERO);
		assertNotNull(entry);
		assertEquals(3.0, entry.z, EPS);
		assertNotNull(DOME.entry(CENTRE, SOUTH, new Vec3(-5, 0, 0), new Vec3(5, 0, 0)), "straight through");
	}

	@Test
	void domeLetsPathsFromInsideOrPastItThrough() {
		assertNull(DOME.entry(CENTRE, SOUTH, new Vec3(1, 0, 0), new Vec3(10, 0, 0)), "from inside");
		assertNull(DOME.entry(CENTRE, SOUTH, new Vec3(-5, 4, 0), new Vec3(5, 4, 0)), "above it");
		assertNull(DOME.entry(CENTRE, SOUTH, new Vec3(0, 0, 10), new Vec3(0, 0, 5)), "stops short");
	}
}
