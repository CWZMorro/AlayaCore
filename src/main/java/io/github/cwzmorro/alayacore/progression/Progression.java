package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.combat.ServantBonus;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.servant.ServantClassConfig;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Applies persistent servant attribute modifiers from synchronization and true-name progress.
 * Refreshes when progression inputs change; removes the modifiers for humans.
 */
public final class Progression {
	private static final Identifier MODIFIER = AlayaCore.id("servant_stats");
	private static final List<Holder<Attribute>> STAT_ATTRIBUTES = List.of(Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE,
		Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE, Attributes.MOVEMENT_SPEED);

	private Progression() {
	}

	public static void register() {
		ServerPlayerEvents.JOIN.register(Progression::refresh);
		ServerPlayerEvents.AFTER_RESPAWN.register(AlayaCore.AFTER_ATTACHMENTS_COPIED, (oldPlayer, newPlayer, alive) -> refresh(newPlayer));
	}

	public static void refreshAll(MinecraftServer server) {
		server.getPlayerList().getPlayers().forEach(Progression::refresh);
	}

	public static void refresh(ServerPlayer player) {
		double maxHealthBefore = player.getMaxHealth();
		Optional<ServantClassConfig> servantClass = AlayaServants.classConfig(player);
		if (servantClass.isEmpty()) {
			STAT_ATTRIBUTES.forEach(attribute -> remove(player, attribute));
		} else {
			ServantStats stats = stats(player, servantClass.get());
			double baseMaxHealth = player.getAttributeBaseValue(Attributes.MAX_HEALTH);
			set(player, Attributes.MAX_HEALTH, stats.maxHp() - baseMaxHealth, AttributeModifier.Operation.ADD_VALUE);
			refreshMelee(player);
			set(player, Attributes.ARMOR, stats.armor(), AttributeModifier.Operation.ADD_VALUE);
			set(player, Attributes.ARMOR_TOUGHNESS, stats.toughness(), AttributeModifier.Operation.ADD_VALUE);
			set(player, Attributes.KNOCKBACK_RESISTANCE, stats.knockbackResistance(), AttributeModifier.Operation.ADD_VALUE);
			set(player, Attributes.MOVEMENT_SPEED, Percent.toFraction(stats.speedPercent()), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		}

		double maxHealthAfter = player.getMaxHealth();
		if (maxHealthAfter > maxHealthBefore) {
			player.heal((float) (maxHealthAfter - maxHealthBefore));
		} else if (player.getHealth() > maxHealthAfter) {
			player.setHealth((float) maxHealthAfter);
		}
		ClassGrades.refresh(player);
	}

	public static double sync(LivingEntity entity) {
		ManaData mana = AlayaMana.get(entity);
		return AlayaConfig.progression().sync(mana == null ? 0.0 : mana.max());
	}

	public static ClassGrade grade(LivingEntity entity) {
		return AlayaConfig.progression().grade(sync(entity), entity instanceof ServerPlayer player && TrueName.isRealized(player));
	}

	public static ServantStats stats(LivingEntity entity, ServantClassConfig servantClass) {
		ProgressionConfig config = AlayaConfig.progression();
		double fill = config.barFill(sync(entity), entity instanceof ServerPlayer player && TrueName.isRealized(player));
		return ServantStats.between(config.demi(), servantClass.real(), fill);
	}

	/**
	 * Refreshes the class attack modifier before a melee hit so crits and buffs scale it with weapon damage.
	 * Removes the modifier from entities without a servant.
	 */
	public static void refreshMelee(LivingEntity attacker) {
		if (AlayaServants.servantId(attacker).isPresent()) {
			set(attacker, Attributes.ATTACK_DAMAGE, ServantBonus.melee(attacker), AttributeModifier.Operation.ADD_VALUE);
		} else {
			remove(attacker, Attributes.ATTACK_DAMAGE);
		}
	}

	private static void set(LivingEntity entity, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(MODIFIER);
		if (current != null && current.amount() == amount && current.operation() == operation) {
			return;
		}
		instance.removeModifier(MODIFIER);
		instance.addPermanentModifier(new AttributeModifier(MODIFIER, amount, operation));
	}

	private static void remove(LivingEntity entity, Holder<Attribute> attribute) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(MODIFIER);
		}
	}
}
