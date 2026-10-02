package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
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
 * Applies a servant player's stats (plan 13.2, 13.7): sync from max mana and the true name fill the
 * bar, and every stat sits that far between its demi value and the class's real value. The stats
 * are attribute modifiers, saved with the player. Humans get none.
 *
 * <p>{@link #refresh} runs whenever an input changes: joining, respawning, choosing a servant, max
 * mana changing, the true name being realized and the config being reloaded.
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

	/** Recomputes the player's stats. A higher max HP also raises current HP by the same amount. */
	public static void refresh(ServerPlayer player) {
		double maxHealthBefore = player.getMaxHealth();
		Optional<ServantClassConfig> servantClass = AlayaServants.classConfig(player);
		if (servantClass.isEmpty()) {
			STAT_ATTRIBUTES.forEach(attribute -> remove(player, attribute));
		} else {
			ServantStats stats = stats(player, servantClass.get());
			AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
			double baseMaxHealth = maxHealth == null ? 0.0 : maxHealth.getBaseValue();
			set(player, Attributes.MAX_HEALTH, stats.maxHp() - baseMaxHealth, AttributeModifier.Operation.ADD_VALUE);
			set(player, Attributes.ATTACK_DAMAGE, stats.attack(), AttributeModifier.Operation.ADD_VALUE);
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

	/** The entity's sync, 0 to 1, from its max mana (plan 13.2); 0 without mana. */
	public static double sync(LivingEntity entity) {
		ManaData mana = AlayaMana.get(entity);
		return AlayaConfig.progression().sync(mana == null ? 0.0 : mana.max());
	}

	/** The servant's current stats. */
	public static ServantStats stats(ServerPlayer player, ServantClassConfig servantClass) {
		ProgressionConfig config = AlayaConfig.progression();
		double fill = config.barFill(sync(player), TrueName.isRealized(player));
		return ServantStats.between(config.demi(), servantClass.real(), fill);
	}

	private static void set(ServerPlayer player, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
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

	private static void remove(ServerPlayer player, Holder<Attribute> attribute) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(MODIFIER);
		}
	}
}
