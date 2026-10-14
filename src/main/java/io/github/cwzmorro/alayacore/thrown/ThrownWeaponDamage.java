package io.github.cwzmorro.alayacore.thrown;

import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.item.WeaponStats;
import io.github.cwzmorro.alayacore.shield.Shields;
import io.github.cwzmorro.alayacore.util.Owners;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Applies thrown weapon damage, enchantments and durability wear, including spear speed and mace fall bonuses.
 */
public final class ThrownWeaponDamage {
	public static final int VANILLA_HIT_IMMUNITY_TICKS = 10;
	// What vanilla sets invulnerableTime to on a hit; hits are refused while it is above VANILLA_HIT_IMMUNITY_TICKS.
	private static final int INVULNERABLE_TIME_ON_HIT = 2 * VANILLA_HIT_IMMUNITY_TICKS;
	// Vanilla's mace curve ({@code MaceItem.getAttackDamageBonus}, kept there in local literals): this much a block of fall up to
	// the first height, then less, then less again.
	private static final double MACE_FIRST_HEIGHT = 3.0;
	private static final double MACE_SECOND_HEIGHT = 8.0;
	private static final double MACE_FIRST_PER_BLOCK = 4.0;
	private static final double MACE_SECOND_PER_BLOCK = 2.0;
	private static final double MACE_LAST_PER_BLOCK = 1.0;
	private static final double KNOCKBACK_SCALE = 0.5;
	private static final float CHANNELING_THUNDER_VOLUME = 5.0F;

	private ThrownWeaponDamage() {
	}

	static boolean strike(ServerLevel level, ThrownWeaponEntity weapon, Entity target, Vec3 velocity) {
		return strike(level, weapon, target, null, velocity);
	}

	static boolean strike(ServerLevel level, ThrownWeaponEntity weapon, Shields.Hit shield, Vec3 velocity) {
		return strike(level, weapon, shield.shield(), shield, velocity);
	}

	private static boolean strike(ServerLevel level, ThrownWeaponEntity thrown, Entity target, Shields.@Nullable Hit shield, Vec3 velocity) {
		// Its thrower, player or mob, is the attacker: kill credit and servant damage rules follow it, as vanilla's arrows do.
		LivingEntity owner = Owners.find(level, thrown.owner());
		ServerPlayer player = owner instanceof ServerPlayer thrower ? thrower : null;
		ItemStack weapon = thrown.item().copy();
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(thrown.damageType()), thrown, owner);
		Float fixed = thrown.fixedDamage();
		float damage = fixed != null ? fixed : damage(level, weapon, target, source, velocity, thrown.thrownFromY() - thrown.getY());
		if (target.invulnerableTime <= INVULNERABLE_TIME_ON_HIT - AlayaConfig.thrownWeapons().hitImmunityTicks()) {
			target.invulnerableTime = 0;
		}
		boolean hurt = shield == null ? target.hurtServer(level, source, damage) : shield.shield().hurtAt(level, source, damage, shield.entry());
		if (hurt) {
			float knockback = EnchantmentHelper.modifyKnockback(level, weapon, target, source, 0.0F);
			if (knockback > 0 && target instanceof LivingEntity living) {
				living.knockback(knockback * KNOCKBACK_SCALE, -velocity.x, -velocity.z);
			}
			EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, weapon);
			channel(level, weapon, target, player);
		}
		wear(level, weapon, player);
		thrown.setItem(weapon);
		return weapon.isEmpty();
	}

	// Vanilla's Channeling only fires for a trident entity, so it is applied here for any thrown weapon with the enchantment.
	private static void channel(ServerLevel level, ItemStack weapon, Entity target, @Nullable ServerPlayer owner) {
		Holder<Enchantment> channeling = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.CHANNELING);
		BlockPos at = target.blockPosition();
		if (EnchantmentHelper.getItemEnchantmentLevel(channeling, weapon) <= 0 || !level.isThundering() || !level.canSeeSky(at)) {
			return;
		}
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.spawn(level, at, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setCause(owner);
		}
		level.playSound(null, at, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.WEATHER, CHANNELING_THUNDER_VOLUME, 1.0F);
	}

	private static float damage(ServerLevel level, ItemStack weapon, Entity target, DamageSource source, Vec3 velocity, double fell) {
		double damage = WeaponStats.attackDamage(weapon);
		KineticWeapon kinetic = weapon.get(DataComponents.KINETIC_WEAPON);
		if (kinetic != null) {
			// Vanilla's spear: the player's own attack, plus the speed it closes on the target at (blocks a second) times the spear's multiplier.
			double closing = Math.max(0.0, (velocity.length() - target.getDeltaMovement().dot(velocity.normalize())) * SharedConstants.TICKS_PER_SECOND);
			double speedDamage = WeaponStats.PLAYER_ATTACK_DAMAGE + Mth.floor(closing * kinetic.damageMultiplier());
			damage = Math.max(damage, speedDamage / AlayaConfig.thrownWeapons().spearDivisor());
		}
		if (weapon.getItem() instanceof MaceItem && fell > MaceItem.SMASH_ATTACK_FALL_THRESHOLD) {
			damage += maceBonus(fell) + EnchantmentHelper.modifyFallBasedDamage(level, weapon, target, source, 0.0F) * fell;
		}
		return EnchantmentHelper.modifyDamage(level, weapon, target, source, (float) damage);
	}

	private static double maceBonus(double fell) {
		if (fell <= MACE_FIRST_HEIGHT) {
			return MACE_FIRST_PER_BLOCK * fell;
		}
		double first = MACE_FIRST_PER_BLOCK * MACE_FIRST_HEIGHT;
		if (fell <= MACE_SECOND_HEIGHT) {
			return first + MACE_SECOND_PER_BLOCK * (fell - MACE_FIRST_HEIGHT);
		}
		return first + MACE_SECOND_PER_BLOCK * (MACE_SECOND_HEIGHT - MACE_FIRST_HEIGHT) + MACE_LAST_PER_BLOCK * (fell - MACE_SECOND_HEIGHT);
	}

	private static void wear(ServerLevel level, ItemStack weapon, @Nullable ServerPlayer owner) {
		if (!weapon.isDamageableItem()) {
			return;
		}
		Weapon stats = weapon.get(DataComponents.WEAPON);
		weapon.hurtAndBreak(stats == null ? 1 : stats.itemDamagePerAttack(), level, owner, item -> {
		});
	}
}
