package io.github.cwzmorro.alayacore.combat;

import io.github.cwzmorro.alayacore.gamerule.AlayaGameRules;
import io.github.cwzmorro.alayacore.shield.Shields;
import io.github.cwzmorro.alayacore.shield.SolidShields;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Moves a body along a dash path, or clears the path without moving it, stopping at walls and solid shields.
 * Never carves what lies behind a shield.
 */
public final class DashMovement {
	private DashMovement() {
	}

	/**
	 * Carves the body's swept volume without moving it, stopping at obstacles and shields.
	 * Measures shield protection from the body's starting position.
	 */
	public static void carve(ServerLevel level, LivingEntity owner, Vec3 offset, double stepSize) {
		Vec3 direction = offset.normalize();
		double distance = offset.length();
		Vec3 from = owner.getBoundingBox().getCenter();
		for (double step = Math.min(stepSize, distance); step <= distance && distance > 0;
			step = Math.min(distance, step + stepSize)) {
			AABB body = owner.getBoundingBox().move(direction.scale(step));
			breakBlocks(level, body, from);
			if (!level.noCollision(owner, body) || SolidShields.collides(owner, body)) {
				break;
			}
			if (step == distance) {
				break;
			}
		}
	}

	private static void breakBlocks(ServerLevel level, AABB body, Vec3 from) {
		if (!level.getGameRules().get(AlayaGameRules.WEAPON_BLOCK_DAMAGE)) {
			return;
		}
		List<BlockPos> swept = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY, body.minZ),
			BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
			swept.add(pos.immutable());
		}
		for (BlockPos pos : Shields.unshielded(level, from, swept)) {
			WeaponBlocks.breakWithoutDrops(level, pos);
		}
	}

	public static Vec3 advance(ServerLevel level, LivingEntity owner, Vec3 offset, double stepSize, boolean carve) {
		Vec3 start = owner.position();
		Vec3 last = start;
		double distance = offset.length();
		Vec3 direction = offset.normalize();
		for (double step = Math.min(stepSize, distance); step <= distance && distance > 0;
			step = Math.min(distance, step + stepSize)) {
			Vec3 next = start.add(direction.scale(step));
			AABB body = owner.getBoundingBox().move(next.subtract(start));
			if (carve) {
				// Nothing behind a shield breaks, measured from where the body was a step ago.
				breakBlocks(level, body, owner.getBoundingBox().move(last.subtract(start)).getCenter());
			}
			if (!level.noCollision(owner, body) || SolidShields.collides(owner, body)) {
				break;
			}
			last = next;
			if (step == distance) {
				break;
			}
		}
		if (owner instanceof ServerPlayer player) {
			player.teleportTo(last.x, last.y, last.z);
		} else {
			owner.setPos(last);
		}
		owner.setDeltaMovement(Vec3.ZERO);
		owner.fallDistance = 0;
		return last;
	}
}
