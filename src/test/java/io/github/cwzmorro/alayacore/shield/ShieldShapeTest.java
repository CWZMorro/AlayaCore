package io.github.cwzmorro.alayacore.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ShieldShapeTest {
	private static final double EPS = 1e-9;
	private static final Vec3 CENTRE = Vec3.ZERO;
	// Faces +Z: attacks come from z > 0.
	private static final Vec3 SOUTH = new Vec3(0, 0, 1);
	private static final ShieldShape DISC = new ShieldShape.Disc(2.5);
	private static final ShieldShape DOME = new ShieldShape.Dome(3);
	// Tip at the centre, 4 blocks long behind it, 0.5 wide at the tip and 2 at the open base.
	private static final ShieldShape CONE = new ShieldShape.Cone(4, 2, 0.5);

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
	void discExplosionPointProjectsOntoThePlaneAndClampsToTheRim() {
		assertEquals(new Vec3(1, 0, 0), DISC.nearestPoint(CENTRE, SOUTH, new Vec3(1, 0, 4)));
		assertEquals(new Vec3(2.5, 0, 0), DISC.nearestPoint(CENTRE, SOUTH, new Vec3(4, 0, 1)));
		assertEquals(CENTRE, DISC.nearestPoint(CENTRE, SOUTH, new Vec3(0, 0, 4)));
	}

	@Test
	void tiltedDiscExplosionPointStillLiesOnTheDisc() {
		Vec3 facing = new Vec3(1, 1, 1).normalize();
		Vec3 point = DISC.nearestPoint(CENTRE, facing, new Vec3(5, 2, 7));
		assertEquals(0.0, point.dot(facing), EPS);
		assertEquals(DISC.radius(), point.length(), EPS);
	}

	@Test
	void coneStopsPathsThatEnterItsTipOrSkin() {
		Vec3 tip = CONE.entry(CENTRE, SOUTH, new Vec3(0, 0, 4), new Vec3(0, 0, -4));
		assertNotNull(tip, "through the tip");
		assertEquals(0.0, tip.z, EPS);
		// Slanting in toward the axis: it meets the skin where the cone is 8/7 wide, 12/7 behind the tip.
		Vec3 skin = CONE.entry(CENTRE, SOUTH, new Vec3(2, 0, 0), new Vec3(0, 0, -4));
		assertNotNull(skin, "through the skin");
		assertEquals(8.0 / 7.0, skin.x, EPS);
		assertEquals(-12.0 / 7.0, skin.z, EPS);
	}

	@Test
	void coneStopsPathsCrossingItSideOn() {
		// Exactly perpendicular to the axis, 2 behind the tip, where the cone is 1.25 wide.
		Vec3 skin = CONE.entry(CENTRE, SOUTH, new Vec3(3, 0, -2), new Vec3(-3, 0, -2));
		assertNotNull(skin, "side-on path");
		assertEquals(1.25, skin.x, EPS);
		assertEquals(-2.0, skin.z, EPS);
	}

	@Test
	void coneLetsPathsFromInsideOutsideOrBehindThrough() {
		assertNull(CONE.entry(CENTRE, SOUTH, new Vec3(0, 0, -2), new Vec3(0, 0, -6)), "from inside");
		assertNull(CONE.entry(CENTRE, SOUTH, new Vec3(3, 0, 0), new Vec3(3, 0, -4)), "past its side");
		assertNull(CONE.entry(CENTRE, SOUTH, new Vec3(0, 0, -8), new Vec3(0, 0, -2)), "through the open base");
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
